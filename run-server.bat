@echo off
rem Запуск сервера. Параметры можно переопределить: run-server.bat --server.port=6000
java -jar server\target\insurance-server.jar %*
pause
