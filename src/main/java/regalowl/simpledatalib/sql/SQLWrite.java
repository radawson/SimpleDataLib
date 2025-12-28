package regalowl.simpledatalib.sql;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;










import regalowl.simpledatalib.SimpleDataLib;
import regalowl.simpledatalib.events.LogEvent;
import regalowl.simpledatalib.events.LogLevel;
import regalowl.simpledatalib.sql.WriteResult.WriteResultType;

public class SQLWrite {

	private SimpleDataLib sdl;
	private ConnectionPool pool;
	
	private ConcurrentHashMap<Long, WriteStatement> writeQueue = new ConcurrentHashMap<Long, WriteStatement>();
	private AtomicLong queueCounter = new AtomicLong();
	private AtomicLong processNext = new AtomicLong();
	
	private AtomicBoolean logWriteErrors = new AtomicBoolean();;
    private AtomicBoolean logSQL = new AtomicBoolean();
    
	private AtomicBoolean writeActive = new AtomicBoolean();
    private ScheduledFuture<?> writeTaskFuture;

    private AtomicBoolean writeSync = new AtomicBoolean();
    private SyncSQLWrite ssw;
    
    private ScheduledExecutorService scheduler;
    
	public SQLWrite(SimpleDataLib sdl, ConnectionPool pool, long writeTaskInterval) {
		this.sdl = sdl;
		this.pool = pool;
		ssw = new SyncSQLWrite(pool, this);
		logWriteErrors.set(true);
		logSQL.set(false);
		queueCounter.set(0);
		processNext.set(0);
		writeActive.set(false);
		writeSync.set(false);
		
		// Use ScheduledExecutorService instead of Timer
		scheduler = new ScheduledThreadPoolExecutor(1, r -> {
			Thread t = new Thread(r, "SimpleDataLib-WriteTask");
			t.setDaemon(true);
			return t;
		});
		writeTaskFuture = scheduler.scheduleWithFixedDelay(new WriteTask(), writeTaskInterval, writeTaskInterval, TimeUnit.MILLISECONDS);
	}
	
	

	public void addToQueue(WriteStatement statement) {
		if (statement == null) {return;}
		if (writeSync.get()) {
			ssw.addToQueue(statement);
		} else {
			writeQueue.put(queueCounter.getAndIncrement(), statement);
		}
	}
	public void addWriteStatementsToQueue(List<WriteStatement> statements) {
		if (statements == null) {return;}
		for (WriteStatement statement : statements) {
			addToQueue(statement);
		}
	}
	public void addToQueue(String statement) {
		if (statement == null) {return;}
		addToQueue(new WriteStatement(statement, sdl));
	}
	public void addToQueue(List<String> statements) {
		if (statements == null) {return;}
		for (String statement : statements) {
			addToQueue(statement);
		}
	}
	public void addToQueue(String statement, ArrayList<Object> parameters) {
		if (statement == null) {return;}
		WriteStatement ws = new WriteStatement(statement, sdl);
		for (Object param:parameters) {
			ws.addParameter(param);
		}
		addToQueue(ws);
	}

	
    private class WriteTask implements Runnable {
    	private final ArrayList<WriteStatement> writeData = new ArrayList<>();
    	
    	@Override
		public synchronized void run() {
			if (writeQueue.isEmpty()) {return;}
			writeActive.set(true);
			DatabaseConnection database = pool.getDatabaseConnection();
			if (database == null) {
				writeActive.set(false);
				return;
			}
			
			try {
				writeData.clear();
				while (!writeQueue.isEmpty()) {
					WriteStatement currentStatement = writeQueue.get(processNext.get());
					if (currentStatement != null) {
						writeData.add(currentStatement);
					}
					writeQueue.remove(processNext.getAndIncrement());
				}
				write(database);
			} finally {
				pool.returnConnection(database);
				writeActive.set(false);
			}
		}
		
		private synchronized void write(DatabaseConnection database) {
			ArrayList<WriteStatement> writeDataCopy = new ArrayList<>();
			writeDataCopy.addAll(writeData);
			WriteResult result = database.write(writeDataCopy);
			if (result.getStatus() == WriteResultType.SUCCESS && logSQL.get() && result.hasSuccessfulSQL()) {
				for (WriteStatement ws:result.getSuccessfulSQL()) {
					ws.logStatement();
				}
			} else if (result.getStatus() == WriteResultType.ERROR && logWriteErrors.get()) {
				result.getFailedStatement().writeFailed(result.getException());
				if (result.hasRemainingSQL()) {
					addWriteStatementsToQueue(result.getRemainingSQL());
				}
			}
			writeDataCopy.clear();
		}
		
		public synchronized ArrayList<WriteStatement> getActiveStatements() {
			return new ArrayList<>(writeData);
		}
    }
	
	


	public int getBufferSize() {
		return writeQueue.size();
	}



	public synchronized void shutDown() {
		if (writeTaskFuture != null) {
			writeTaskFuture.cancel(false);
		}
		if (scheduler != null) {
			scheduler.shutdown();
			try {
				if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
					scheduler.shutdownNow();
				}
			} catch (InterruptedException e) {
				scheduler.shutdownNow();
				Thread.currentThread().interrupt();
			}
		}
		
		// Get active statements from the last write task
		WriteTask lastTask = new WriteTask();
		addWriteStatementsToQueue(lastTask.getActiveStatements());
		
		DatabaseConnection dbConnection = pool.getDatabaseConnection();
		if (dbConnection != null) {
			saveQueue(dbConnection);
			pool.returnConnection(dbConnection);
		}
	}

	private void saveQueue(DatabaseConnection database) {
		if (writeQueue.size() == 0) {return;}
		sdl.getEventPublisher().fireEvent(new LogEvent("[" + sdl.getName() + "]Saving the remaining SQL queue: [" + writeQueue.size() + " statements].  Please wait.", null, LogLevel.INFO));
		ArrayList<WriteStatement> writeArray = new ArrayList<WriteStatement>();
		while (writeQueue.size() > 0) {
			writeArray.add(writeQueue.get(processNext.get()));
			writeQueue.remove(processNext.getAndIncrement());
		}
		WriteResult result = database.write(writeArray);
		if (result.getStatus() == WriteResultType.SUCCESS) {
			if (logSQL.get() && result.hasSuccessfulSQL()) {
				for (WriteStatement ws : result.getSuccessfulSQL()) {
					ws.logStatement();
				}
			}
		} else if (result.getStatus() == WriteResultType.ERROR) {
			sdl.getEventPublisher().fireEvent(new LogEvent("[" + sdl.getName() + "]A database error occurred while shutting down.  Attempting to save remaining data... This may take longer than usual.", null, LogLevel.SEVERE));
			if (logWriteErrors.get()) result.getFailedStatement().logError(result.getException());
			if (result.hasRemainingSQL()) {
				for (WriteStatement ws : result.getRemainingSQL()) {
					ArrayList<WriteStatement> statement = new ArrayList<WriteStatement>();
					statement.add(ws);
					WriteResult r = database.write(statement);
					if (r.getStatus() == WriteResultType.SUCCESS) {
						if (logSQL.get() && r.hasSuccessfulSQL()) r.getSuccessfulSQL().get(0).logStatement();
					} else if (r.getStatus() == WriteResultType.ERROR) {
						if (logWriteErrors.get()) r.getFailedStatement().logError(r.getException());
					}
				}
			}
		}
		writeQueue.clear();
		sdl.getEventPublisher().fireEvent(new LogEvent("[" + sdl.getName() + "]SQL queue save complete.", null, LogLevel.INFO));
	}



	
	public void performInsert(String table, HashMap<String, String> values) {
		addToQueue(getInsertStatement(table, values));
	}
	public void performUpdate(String table, HashMap<String, String> values, HashMap<String, String> conditions) {
		addToQueue(getUpdateStatement(table, values, conditions));
	}
	public void performDelete(String table, HashMap<String, String> conditions) {
		addToQueue(getDeleteStatement(table, conditions));
	}
	
	public WriteStatement getInsertStatement(String table, HashMap<String, String> values) {
		String statement = "INSERT INTO " + table + " (";
		for (String field:values.keySet()) {
			statement += field + ", ";
		}
		statement = statement.substring(0, statement.length() - 2);
		statement += ") VALUES (";
		for (int i=0; i<values.size(); i++) {
			statement +=  "?,";
		}
		statement = statement.substring(0, statement.length() - 1);
		statement += ")";
		WriteStatement ws = new WriteStatement(statement, sdl);
		for (String value:values.values()) {
			ws.addParameter(value);
		}
		return ws;
	}
	public WriteStatement getUpdateStatement(String table, HashMap<String, String> values, HashMap<String, String> conditions) {
		String statement = "UPDATE " + table + " SET ";
		Iterator<String> it = values.keySet().iterator();
		while (it.hasNext()) {
			statement += it.next() + " = ?, ";
		}
		statement = statement.substring(0, statement.length() - 2);
		statement += " WHERE ";
		it = conditions.keySet().iterator();
		while (it.hasNext()) {
			String field = it.next();
			statement += field + " = ? AND ";
		}
		statement = statement.substring(0, statement.length() - 5);
		WriteStatement ws = new WriteStatement(statement, sdl);
		it = values.keySet().iterator();
		while (it.hasNext()) {
			String field = it.next();
			ws.addParameter(values.get(field));
		}
		it = conditions.keySet().iterator();
		while (it.hasNext()) {
			String field = it.next();
			ws.addParameter(conditions.get(field));
		}
		return ws;
	}
	public WriteStatement getDeleteStatement(String table, HashMap<String, String> conditions) {
		String statement = "DELETE FROM " + table + " WHERE ";
		Iterator<String> it = conditions.keySet().iterator();
		while (it.hasNext()) {
			String field = it.next();
			statement += field + " = ? AND ";
		}
		statement = statement.substring(0, statement.length() - 5);
		WriteStatement ws = new WriteStatement(statement, sdl);
		
		it = conditions.keySet().iterator();
		while (it.hasNext()) {
			String field = it.next();
			ws.addParameter(conditions.get(field));
		}
		return ws;
	}
	
	
	
	
	public void setErrorLogging(boolean state) {
		logWriteErrors.set(state);
	}
	public boolean logWriteErrors() {
		return logWriteErrors.get();
	}
	public boolean writeActive() {
		return writeActive.get();
	}
	public void setLogSQL(boolean state) {
		logSQL.set(state);
	}
	public boolean logSQL() {
		return logSQL.get();
	}
	public boolean writeSync() {
		return writeSync.get();
	}
	public void writeSync(boolean state) {
		writeSync.set(state);
	}
	public int getSyncQueueSize() {
		return ssw.getQueueSize();
	}
	public void writeSyncQueue() {
		ssw.writeQueue();
	}
	
	/**
	 * Asynchronously writes a statement and returns a CompletableFuture with the result.
	 * This is a modern async API that can be used alongside the traditional queue-based approach.
	 * 
	 * @param statement The write statement to execute
	 * @return CompletableFuture that completes with the WriteResult
	 */
	public CompletableFuture<WriteResult> writeAsync(WriteStatement statement) {
		return CompletableFuture.supplyAsync(() -> {
			DatabaseConnection dbConnection = pool.getDatabaseConnection();
			if (dbConnection == null) {
				return new WriteResult(WriteResultType.ERROR);
			}
			try {
				List<WriteStatement> statements = new ArrayList<>();
				statements.add(statement);
				return dbConnection.write(statements);
			} finally {
				pool.returnConnection(dbConnection);
			}
		}, scheduler != null ? scheduler : java.util.concurrent.ForkJoinPool.commonPool());
	}
	
	/**
	 * Asynchronously writes multiple statements and returns a CompletableFuture with the result.
	 * 
	 * @param statements The write statements to execute
	 * @return CompletableFuture that completes with the WriteResult
	 */
	public CompletableFuture<WriteResult> writeAsync(List<WriteStatement> statements) {
		return CompletableFuture.supplyAsync(() -> {
			DatabaseConnection dbConnection = pool.getDatabaseConnection();
			if (dbConnection == null) {
				return new WriteResult(WriteResultType.ERROR);
			}
			try {
				return dbConnection.write(statements);
			} finally {
				pool.returnConnection(dbConnection);
			}
		}, scheduler != null ? scheduler : java.util.concurrent.ForkJoinPool.commonPool());
	}

}
