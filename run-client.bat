@echo off
rem Запуск клиентского приложения. Адрес сервера: --server.host=192.168.1.10 --server.port=5555
start "" javaw -jar client\target\insurance-client.jar %*
