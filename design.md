# Проектные решения: ПС «Страховой сервис»

Тема: программное средство для поддержки процесса оказания страховых услуг населению.
Рабочее название ПС: «InsureDesk» (страховой сервис для физических лиц). Валюта — BYN.

## 1 Предметная область (кратко)

Страховая компания продаёт физическим лицам добровольные виды страхования:
страхование квартиры (имущества), КАСКО, страхование от несчастных случаев,
страхование медицинских расходов выезжающих за рубеж. Процесс: консультация и расчёт
стоимости → заявка → андеррайтинг (проверка) агентом → заключение договора (полиса) →
уплата страховых взносов по графику → сопровождение (продление, расторжение) →
урегулирование страховых случаев (заявление, проверка, расчёт и выплата возмещения) →
отчётность и анализ убыточности.

## 2 Роли

| Роль | Кто | Основное |
|---|---|---|
| Клиент | физическое лицо, страхователь | расчёт стоимости, заявки, свои полисы и платежи, заявления о страховых случаях |
| Агент | сотрудник компании | рассмотрение заявок, оформление полисов, урегулирование убытков, продление/расторжение, контроль просрочек |
| Администратор | руководитель/администратор системы | виды страхования и тарифы, учётные записи, отчёты и аналитика |

Авторизация обязательна для всех ролей (кроме регистрации клиента).

## 3 Варианты использования (высокоуровневые, без тривиального CRUD)

| № | Вариант использования | Актор |
|---|---|---|
| UC1 | Регистрация клиента | Гость |
| UC2 | Авторизация | все |
| UC3 | Расчёт стоимости страхования (калькулятор премии) | Клиент, Агент |
| UC4 | Подача заявки на страхование | Клиент |
| UC5 | Рассмотрение заявки и оформление полиса (расчёт премии, график взносов) | Агент |
| UC6 | Внесение страхового взноса по графику | Клиент |
| UC7 | Подача заявления о страховом случае | Клиент |
| UC8 | Урегулирование страхового случая с расчётом страховой выплаты | Агент |
| UC9 | Продление договора страхования | Агент |
| UC10 | Досрочное расторжение договора с расчётом возврата части премии | Агент |
| UC11 | Контроль просроченных взносов и истекающих договоров | Агент |
| UC12 | Просмотр своих договоров, графика взносов и статуса выплат | Клиент |
| UC13 | Управление видами страхования и тарифами | Администратор |
| UC14 | Управление учётными записями агентов (создание, блокировка) | Администратор |
| UC15 | Отчёт о продажах агентов за период с диаграммой | Администратор |
| UC16 | Анализ убыточности видов страхования с диаграммой | Администратор |
| UC17 | Выгрузка отчёта в текстовый файл | Администратор |

## 4 Ключевые алгоритмы бизнес-логики (раздел 2.3, чертёж 2)

### 4.1 Расчёт страховой премии (PremiumCalculator + стратегии по видам)
П = С × Т × Кв × Кс × Кбм × Кф, где
- С — страховая сумма; Т — базовый тариф вида страхования (доля от суммы в год);
- Кв — возрастной коэффициент страхователя (до 25 лет 1,20; 25–60 лет 1,00; старше 60 лет 1,35);
- Кс — коэффициент срока: срок/12, для сроков меньше года применяется краткосрочная надбавка ×1,1;
- Кбм — бонус-малус по истории клиента: нет выплат за 2 года 0,90; одна выплата 1,00; две и более 1,25;
- Кф — коэффициент франшизы: франшиза 0 % → 1,00; 1 % → 0,95; 2 % → 0,90; 5 % → 0,80.
Стратегия вида страхования может добавить свой множитель (например, КАСКО — возраст ТС).
Проверки: С в пределах [min_sum; max_sum] вида, срок ≤ max_term_months. Округление до 0,01 BYN.
График взносов: единовременно или равными долями (1, 2, 4 или 12 взносов).

### 4.2 Расчёт страховой выплаты (PayoutCalculator)
1. Договор существует и на дату события имел статус «Действует»; дата события в [начало; окончание].
2. Нет просроченных взносов на дату события (иначе отказ).
3. Заявление подано не позднее 30 дней с даты события (иначе отказ).
4. Ущерб У ≤ франшизы Ф → отказ. Иначе В = У − Ф.
5. В ≤ остатку страховой суммы (С − сумма предыдущих выплат по договору); иначе В = остаток.
6. При исчерпании страховой суммы договор переводится в статус «Исполнен».

### 4.3 Возврат части премии при расторжении
Возврат = уплаченная премия × (неистёкшие дни / все дни) × (1 − 0,25 расходы на ведение дела) − выплаты по договору; не меньше 0.

## 5 База данных (PostgreSQL 16), 3НФ

| Таблица | Назначение | Ключевые поля |
|---|---|---|
| roles | роли пользователей | id PK, name UNIQUE |
| users | учётные записи | id PK, login UNIQUE, password_hash, salt, role_id FK→roles, is_blocked, created_at |
| clients | страхователи | id PK, user_id FK→users UNIQUE, last_name, first_name, patronymic, birth_date, passport_no UNIQUE, phone, address |
| agents | страховые агенты | id PK, user_id FK→users UNIQUE, last_name, first_name, patronymic, position, hire_date, commission_rate |
| insurance_types | виды страхования и тарифы | id PK, name UNIQUE, description, base_rate, min_sum, max_sum, max_term_months, is_active |
| applications | заявки клиентов | id PK, client_id FK, type_id FK, agent_id FK NULL, requested_sum, term_months, franchise_percent, installments, object_description, status, decision_comment, created_at, decided_at |
| policies | договоры (полисы) | id PK, policy_number UNIQUE, application_id FK UNIQUE NULL, client_id FK, agent_id FK, type_id FK, insured_sum, premium, franchise, start_date, end_date, status, created_at, terminated_at, refund_amount |
| payments | график и факт взносов | (policy_id FK, installment_no) PK — идентифицирующая связь, amount, due_date, paid_date NULL |
| claims | страховые случаи | id PK, policy_id FK, incident_date, report_date, description, damage_amount, status, payout_amount, agent_id FK NULL, decision_comment, decided_at |

Связи: roles 1:N users; users 1:0..1 clients; users 1:0..1 agents; clients 1:N applications;
insurance_types 1:N applications/policies; agents 1:N policies/claims (неидентифицирующие, agent nullable в заявках/случаях);
applications 1:0..1 policies; policies 1:N payments (идентифицирующая); policies 1:N claims.
Статусы — CHECK-ограничения. Ссылочная целостность: FK с ON DELETE RESTRICT (payments — CASCADE).

## 6 Архитектура

Трёхуровневая клиент-серверная: Swing-клиент ⇄ TCP-сокеты (сериализованные объекты Request/Response) ⇄
многопоточный сервер (ServerSocket + ExecutorService, поток на подключение) ⇄ JDBC (драйвер PostgreSQL) ⇄ PostgreSQL.
Настройки сервера — server.properties и аргументы командной строки; клиента — client.properties.

Модули Maven: `common` (модель, протокол), `server`, `client`.

### Иерархия классов и паттерны
- `Entity` (abstract, Serializable, id) → `Person` (abstract: ФИО) → `Client`, `Agent`;
  `Entity` → `User`, `InsuranceType`, `Application`, `Policy`, `Payment`, `Claim`.
- Исключения: `InsuranceException extends Exception` → `ValidationException`, `AuthException`, `BusinessRuleException`.
- `PremiumStrategy` (interface) → `AbstractPremiumStrategy` → `PropertyPremiumStrategy`, `CascoPremiumStrategy`,
  `AccidentPremiumStrategy`, `TravelPremiumStrategy` — паттерн «Стратегия»; `PremiumStrategyFactory` — «Фабрика».
- `ConnectionManager` — «Одиночка»; `CommandHandler` + `Map<CommandType, CommandHandler>` — «Команда».
- `AbstractDao<T>` → `UserDao`, `ClientDao`, `AgentDao`, `InsuranceTypeDao`, `ApplicationDao`, `PolicyDao`, `PaymentDao`, `ClaimDao` — DAO.
- Клиент: `BaseFrame extends JFrame`, `EntityTableModel<T> extends AbstractTableModel`, `PieChartPanel`/`BarChartPanel extends JPanel`.
- Перегрузка (`calculate(...)` с разным набором параметров), переопределение (`toString`, `applyTypeFactor`), static-методы (`PasswordUtil.hash`, `Money.round`).
