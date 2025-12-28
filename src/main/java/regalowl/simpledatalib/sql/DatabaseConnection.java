package regalowl.simpledatalib.sql;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import regalowl.simpledatalib.SimpleDataLib;
import regalowl.simpledatalib.events.LogEvent;
import regalowl.simpledatalib.events.LogLevel;
import regalowl.simpledatalib.events.ShutdownEvent;
import regalowl.simpledatalib.sql.WriteResult.WriteResultType;

public class DatabaseConnection {

	private SimpleDataLib sdl;
	private Connection connection;
    private AtomicBoolean readOnly = new AtomicBoolean();
    private AtomicBoolean lock = new AtomicBoolean();
    private AtomicBoolean ignoreWrites = new AtomicBoolean();
    
	/**
	 * Constructor for creating a DatabaseConnection with a provided Connection from DataSource.
	 * This is the preferred constructor when using HikariCP.
	 */
	public DatabaseConnection(SimpleDataLib sdl, Connection connection, boolean ignoreWrites) {
		this.lock.set(false);
		this.sdl = sdl;
		this.connection = connection;
		this.readOnly.set(false);
		this.ignoreWrites.set(ignoreWrites);
		try {
			if (connection != null) {
				connection.setAutoCommit(false);
			}
		} catch (SQLException e) {
			sdl.getErrorWriter().writeError(e, "Failed to configure connection");
		}
	}
	
	/**
	 * Legacy constructor for backward compatibility.
	 * @deprecated Use DatabaseConnection(SimpleDataLib, Connection, boolean) instead
	 */
	@Deprecated
	public DatabaseConnection(SimpleDataLib sdl, boolean readOnly) {
		this.lock.set(false);
		this.sdl = sdl;
		this.readOnly.set(readOnly);
		ignoreWrites.set(false);
		openConnection();
	}

	public synchronized WriteResult write(List<WriteStatement> statements) {
		if (statements == null || statements.size() == 0) return new WriteResult(WriteResultType.EMPTY);
		if (lock.get()) return new WriteResult(WriteResultType.DISABLED);
		if (ignoreWrites.get()) return new WriteResult(WriteResultType.SUCCESS);
		WriteStatement currentStatement = null;
		PreparedStatement preparedStatement = null;
		try {
			prepareConnection();
			for (WriteStatement statement : statements) {
				currentStatement = statement;
				preparedStatement = connection.prepareStatement(currentStatement.getStatement());
				currentStatement.applyParameters(preparedStatement);
				preparedStatement.executeUpdate();
			}
			if (lock.get()) {
				connection.rollback();
				return new WriteResult(WriteResultType.DISABLED);
			} else {
				connection.commit();
				WriteResult result = new WriteResult(WriteResultType.SUCCESS);
				result.setSuccessful(statements);
				return result;
			}
		} catch (SQLException e) {
			try {
				connection.rollback();
				statements.remove(currentStatement);
				WriteResult result = new WriteResult(WriteResultType.ERROR);
				result.setFailedStatement(currentStatement);
				result.setRemaining(statements);
				result.setException(e);
				return result;
			} catch (SQLException e1) {
				sdl.getErrorWriter().writeError(e1, "Rollback failed.");
				statements.remove(currentStatement);
				WriteResult result = new WriteResult(WriteResultType.ERROR);
				result.setRemaining(statements);
				result.setException(e1);
				return result;
			}
		} finally {
			try {
				if (preparedStatement != null) preparedStatement.close();
			} catch (SQLException e) {
				sdl.getErrorWriter().writeError(e);
			}
		}
	}
	
	
	public synchronized WriteResult writeWithoutTransaction(WriteStatement statement) {
		if (statement == null) return new WriteResult(WriteResultType.EMPTY);
		if (lock.get()) return new WriteResult(WriteResultType.DISABLED);
		if (ignoreWrites.get()) return new WriteResult(WriteResultType.SUCCESS);
		try {
			prepareConnection();
			connection.setAutoCommit(true);
			Statement state = connection.createStatement();
			state.execute(statement.getStatement());
			state.close();
			WriteResult result = new WriteResult(WriteResultType.SUCCESS);
			result.addSuccessful(statement);
			return result;
		} catch (SQLException e) {
			WriteResult result = new WriteResult(WriteResultType.ERROR);
			result.setFailedStatement(statement);
			result.setException(e);
			return result;
		}
	}
	
	public synchronized QueryResult read(BasicStatement statement) {
		QueryResult qr = new QueryResult();
		PreparedStatement preparedStatement = null;
		ResultSet resultSet = null;
		try {
			prepareConnection();
			preparedStatement = connection.prepareStatement(statement.getStatement());
			statement.applyParameters(preparedStatement);
			resultSet = preparedStatement.executeQuery();
			ResultSetMetaData rsmd = resultSet.getMetaData();
			int columnCount = rsmd.getColumnCount();
			for (int i = 1; i <= columnCount; i++) {
				qr.addColumnName(rsmd.getColumnLabel(i));
			}
			while (resultSet.next()) {
				for (int i = 1; i <= columnCount; i++) {
					qr.addData(i, resultSet.getString(i));
				}
			}
			return qr;
		} catch (SQLException e) {
			qr.setException(e, statement.getStatement());
			return qr;
		} finally {
			try {
				if (preparedStatement != null) preparedStatement.close();
				if (resultSet != null) resultSet.close();
			} catch (SQLException e) {
				sdl.getErrorWriter().writeError(e);
			}
		}
	}
	
	public void setIgnoreWrites(boolean state) {
		ignoreWrites.set(state);
	}

	public void lock() {
		lock.set(true);
	}
	public void unlock() {
		lock.set(false);
	}

	public synchronized void prepareConnection() {
		if (!isValid()) {
			fixConnection();
		}
	}
	
	private synchronized boolean isValid() {
		try {
			if (connection == null || connection.isClosed()) return false;
			if (!readOnly.get() && connection.isReadOnly()) return false;
			if (!readOnly.get()) {
				try {
					connection.setAutoCommit(false);
				} catch (SQLException se) {
					return false;
				}
			}
		} catch (SQLException e) {
			return false;
		}
		return true;
	}
	
	private synchronized void fixConnection() {
		// With HikariCP, connections are managed by the pool
		// If connection is invalid, we should get a new one from the pool
		// For now, log the error and let the pool handle reconnection
		if (readOnly.get()) {
			sdl.getEventPublisher().fireEvent(new LogEvent("[" + sdl.getName() + "]Database connection error. " 
		+ "Make sure your database is unlocked and readable in order to use this plugin.", null, LogLevel.SEVERE));
		} else {
			sdl.getEventPublisher().fireEvent(new LogEvent("[" + sdl.getName() + "]Database connection error. " 
		+ "Make sure your database is unlocked and writeable in order to use this plugin.", null, LogLevel.SEVERE));
		}
		// Don't shutdown on connection errors - let HikariCP handle retries
	}
	
	/**
	 * Legacy method for backward compatibility.
	 * With HikariCP, connections are obtained from the pool, not opened directly.
	 * @deprecated Connections should be obtained from ConnectionPool
	 */
	@Deprecated
	public synchronized void openConnection() {
		// Try to get connection from DataSource if available
		if (sdl.getSQLManager().getDataSource() != null) {
			try {
				connection = sdl.getSQLManager().getDataSource().getConnection();
				connection.setReadOnly(readOnly.get());
				if (!readOnly.get()) {
					connection.setAutoCommit(false);
				}
			} catch (Exception e) {
				sdl.getErrorWriter().writeError(e, "Database connection error.");
			}
		} else {
			// Fallback to old method if DataSource not available (shouldn't happen)
			sdl.getErrorWriter().writeError(new IllegalStateException("DataSource not available"), "Cannot open connection without DataSource");
		}
	}
	
	public synchronized void closeConnection() {
		if (connection == null) return;
		try {
			if (!connection.getAutoCommit()) {
				connection.rollback();
			}
		} catch (SQLException e) {
			// Ignore rollback errors on close
		}
		try {
			if (!connection.isClosed()) {
				connection.close(); // Returns connection to HikariCP pool
			}
		} catch (Exception e) {
			sdl.getErrorWriter().writeError(e, "Connection failed to close.");
		}
		connection = null;
	}

}
