@echo off
setlocal

:: Install PostgreSQL silently
echo Installing PostgreSQL...
start /wait postgresql-17.2-3-windows-x64.exe --mode unattended --superpassword hello123 --datadir "C:\Program Files\PostgreSQL\17\data"

:: Add PostgreSQL bin directory to PATH for running commands
set PATH=%PATH%;C:\Program Files\PostgreSQL\17\bin

:: Wait a moment for PostgreSQL installation to complete
timeout /t 10 /nobreak

:: Get the current directory where the batch script is located
set "current_dir=%~dp0"

:: Check if PostgreSQL is running (can be skipped if you are sure it's started automatically)
echo Checking PostgreSQL service status...
pg_ctl status -D "C:\Program Files\PostgreSQL\17\data"

:: Restore the database from the backup using the relative path
echo Restoring PDB database from backup...
pg_restore -U postgres -h localhost -p 5432 -d PDB -v "%current_dir%\Backup\PDB_backup.sql"

echo Database restore completed.

endlocal
pause
