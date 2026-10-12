@echo off
chcp 65001 > nul
rem Создание базы данных insurance и загрузка демонстрационных данных.
rem Пароль пользователя postgres при необходимости задайте в переменной PGPASSWORD.
psql -U postgres -h localhost -f database\01_schema.sql
psql -U postgres -h localhost -f database\02_demo_data.sql
pause
