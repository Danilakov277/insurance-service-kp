package by.bsuir.insurance.server.it;

import by.bsuir.insurance.common.model.Agent;
import by.bsuir.insurance.common.model.Application;
import by.bsuir.insurance.common.model.Claim;
import by.bsuir.insurance.common.model.Client;
import by.bsuir.insurance.common.model.ControlItem;
import by.bsuir.insurance.common.model.InsuranceCategory;
import by.bsuir.insurance.common.model.InsuranceType;
import by.bsuir.insurance.common.model.LossRatioRow;
import by.bsuir.insurance.common.model.Payment;
import by.bsuir.insurance.common.model.PayoutDecision;
import by.bsuir.insurance.common.model.Policy;
import by.bsuir.insurance.common.model.PolicyStatus;
import by.bsuir.insurance.common.model.PremiumQuote;
import by.bsuir.insurance.common.model.RefundCalculation;
import by.bsuir.insurance.common.model.SalesReportRow;
import by.bsuir.insurance.common.model.User;
import by.bsuir.insurance.common.protocol.CommandType;
import by.bsuir.insurance.common.protocol.Request;
import by.bsuir.insurance.common.protocol.Response;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.PrintWriter;
import java.math.BigDecimal;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Сквозной сценарий проверки работоспособности: запускается при работающем сервере
 * и базе с демонстрационными данными. Проходит все варианты использования через
 * сетевой протокол и формирует таблицу результатов (файл, указанный первым аргументом).
 */
public final class EndToEndScenario {
    private static final List<String[]> RESULTS = new ArrayList<>();
    private static final String HOST = System.getProperty("host", "localhost");
    private static final int PORT = Integer.getInteger("port", 5555);
    private static int failed;

    private EndToEndScenario() {
    }

    /** Отдельное соединение с сервером (отдельный «клиент»). */
    static final class Conn implements AutoCloseable {
        final Socket s;
        final ObjectOutputStream out;
        final ObjectInputStream in;

        Conn() throws Exception {
            s = new Socket(HOST, PORT);
            s.setTcpNoDelay(true);
            out = new ObjectOutputStream(new java.io.BufferedOutputStream(s.getOutputStream()));
            out.flush();
            in = new ObjectInputStream(new java.io.BufferedInputStream(s.getInputStream()));
        }

        Response send(Request r) throws Exception {
            out.writeObject(r);
            out.reset();
            out.flush();
            return (Response) in.readObject();
        }

        <T> T ok(Request r) throws Exception {
            Response resp = send(r);
            if (!resp.isSuccess()) {
                throw new IllegalStateException(r.getCommand() + ": " + resp.getMessage());
            }
            return resp.getData();
        }

        User login(String login, String pwd) throws Exception {
            return ok(new Request(CommandType.LOGIN).with("login", login).with("password", pwd));
        }

        @Override
        public void close() throws Exception {
            s.close();
        }
    }

    private static void check(String uc, String action, String expected, String actual, boolean pass) {
        RESULTS.add(new String[] {String.valueOf(RESULTS.size() + 1), uc, action, expected, actual, pass ? "Пройден" : "НЕ ПРОЙДЕН"});
        if (!pass) {
            failed++;
        }
        System.out.println((pass ? "ok   " : "FAIL ") + uc + " " + action + " → " + actual);
    }

    private static void expectError(Conn c, Request r, String uc, String action, String fragment) throws Exception {
        Response resp = c.send(r);
        check(uc, action, "Отказ: «…" + fragment + "…»", resp.isSuccess() ? "Успешно (ошибка не выдана)" : "Отказ: «" + resp.getMessage() + "»",
                !resp.isSuccess() && resp.getMessage().contains(fragment));
    }

    public static void main(String[] args) throws Exception {
        LocalDate today = LocalDate.now();
        String suffix = String.valueOf(System.currentTimeMillis() % 100000);
        String clientLogin = "test" + suffix;
        String passport = "TT" + String.format("%07d", Long.parseLong(suffix));

        try (Conn guest = new Conn(); Conn cl = new Conn(); Conn ag = new Conn(); Conn ad = new Conn()) {
            // ---------- авторизация и регистрация
            expectError(guest, new Request(CommandType.LOGIN).with("login", "admin").with("password", "wrong"),
                    "UC2", "Вход с неверным паролем", "Неверный логин или пароль");
            expectError(guest, new Request(CommandType.GET_MY_POLICIES), "UC2", "Запрос без авторизации",
                    "Требуется авторизация");
            Client data = new Client();
            data.setLastName("Тестов");
            data.setFirstName("Тест");
            data.setPatronymic("Тестович");
            data.setBirthDate(LocalDate.of(1990, 3, 15));
            data.setPassportNo(passport);
            data.setPhone("+375 29 000-00-00");
            data.setAddress("г. Минск, ул. Тестовая, 1");
            Response reg = cl.send(new Request(CommandType.REGISTER_CLIENT).with("login", clientLogin)
                    .with("password", "test123").with("client", data));
            check("UC1", "Регистрация клиента " + clientLogin, "Учётная запись создана, вход выполнен",
                    reg.isSuccess() ? "Создана, роль " + ((User) reg.getData()).getRole().getTitle() : reg.getMessage(), reg.isSuccess());
            expectError(guest, new Request(CommandType.REGISTER_CLIENT).with("login", clientLogin)
                    .with("password", "test123").with("client", data), "UC1", "Повторная регистрация с тем же логином", "уже занят");
            Client young = new Client();
            young.setLastName("Юный");
            young.setFirstName("Иван");
            young.setBirthDate(today.minusYears(16));
            young.setPassportNo("YY1234567");
            young.setPhone("+375 29 111-11-11");
            young.setAddress("г. Минск");
            expectError(guest, new Request(CommandType.REGISTER_CLIENT).with("login", "young" + suffix)
                    .with("password", "test123").with("client", young), "UC1", "Регистрация лица младше 18 лет", "18 лет");

            // ---------- расчёт и заявка
            List<InsuranceType> types = cl.ok(new Request(CommandType.GET_ACTIVE_TYPES));
            check("UC3", "Получение активных видов страхования", "5 видов", types.size() + " видов", types.size() == 5);
            InsuranceType flat = types.get(0);
            Application a = new Application();
            a.setTypeId(flat.getId());
            a.setRequestedSum(new BigDecimal("50000"));
            a.setTermMonths(12);
            a.setFranchisePercent(0);
            a.setInstallments(2);
            a.setObjectDescription("Квартира, 2 комнаты, 54 м²");
            PremiumQuote q = cl.ok(new Request(CommandType.CALCULATE_PREMIUM).with("application", a));
            check("UC3", "Расчёт премии: квартира, 50 000 BYN, 12 мес.", "225,00 BYN (50000×0,005×0,90)",
                    q.getPremium() + " BYN", q.getPremium().compareTo(new BigDecimal("225.00")) == 0);
            Application bad = new Application();
            bad.setTypeId(flat.getId());
            bad.setRequestedSum(new BigDecimal("1000000"));
            bad.setTermMonths(12);
            bad.setInstallments(1);
            bad.setObjectDescription("x");
            expectError(cl, new Request(CommandType.CALCULATE_PREMIUM).with("application", bad), "UC3",
                    "Расчёт с суммой выше максимальной", "Страховая сумма должна быть");
            Application submitted = cl.ok(new Request(CommandType.SUBMIT_APPLICATION).with("application", a));
            check("UC4", "Подача заявки", "Заявка в статусе «На рассмотрении»", submitted.getStatus().getTitle(),
                    "NEW".equals(submitted.getStatus().name()));
            expectError(cl, new Request(CommandType.GET_NEW_APPLICATIONS), "UC2", "Клиент вызывает функцию агента",
                    "Недостаточно прав");

            // ---------- агент: оформление полиса
            ag.login("kovaleva", "agent123");
            List<Application> fresh = ag.ok(new Request(CommandType.GET_NEW_APPLICATIONS));
            boolean seen = false;
            for (Application x : fresh) {
                seen |= x.getId() == submitted.getId();
            }
            check("UC5", "Агент видит новую заявку", "Заявка в списке с расчётной премией", seen ? "Есть в списке" : "Нет", seen);
            Policy policy = ag.ok(new Request(CommandType.APPROVE_APPLICATION).with("applicationId", submitted.getId())
                    .with("startDate", today));
            check("UC5", "Оформление полиса по заявке", "Полис, премия 225,00, «Ожидает оплаты»",
                    policy.getPolicyNumber() + ", " + policy.getPremium() + ", " + policy.getStatus().getTitle(),
                    policy.getStatus() == PolicyStatus.WAITING_PAYMENT && policy.getPremium().compareTo(new BigDecimal("225.00")) == 0);
            expectError(ag, new Request(CommandType.APPROVE_APPLICATION).with("applicationId", submitted.getId())
                    .with("startDate", today), "UC5", "Повторное оформление той же заявки", "уже рассмотрена");

            // ---------- клиент: взносы
            List<Payment> sched = cl.ok(new Request(CommandType.GET_PAYMENTS).with("policyId", policy.getId()));
            check("UC12", "Просмотр графика взносов", "2 взноса по 112,50", sched.size() + " взноса: "
                    + sched.get(0).getAmount() + " и " + sched.get(1).getAmount(), sched.size() == 2
                    && sched.get(0).getAmount().add(sched.get(1).getAmount()).compareTo(new BigDecimal("225.00")) == 0);
            Policy paid = cl.ok(new Request(CommandType.PAY_INSTALLMENT).with("policyId", policy.getId()));
            check("UC6", "Внесение первого взноса", "Договор переходит в «Действует»", paid.getStatus().getTitle(),
                    paid.getStatus() == PolicyStatus.ACTIVE);
            try (Conn other = new Conn()) {
                other.login("ivanov", "client123");
                expectError(other, new Request(CommandType.GET_PAYMENTS).with("policyId", policy.getId()), "UC12",
                        "Другой клиент запрашивает чужой договор", "Нет доступа");
            }

            // ---------- страховой случай
            Claim claim = new Claim();
            claim.setPolicyId(policy.getId());
            claim.setIncidentDate(today);
            claim.setDescription("Залив квартиры соседями");
            claim.setDamageAmount(new BigDecimal("3200"));
            Claim reported = cl.ok(new Request(CommandType.REPORT_CLAIM).with("claim", claim));
            check("UC7", "Заявление о страховом случае", "Зарегистрировано, статус «Заявлен»",
                    reported.getStatus().getTitle(), "REPORTED".equals(reported.getStatus().name()));
            Claim future = new Claim();
            future.setPolicyId(policy.getId());
            future.setIncidentDate(today.plusDays(3));
            future.setDescription("x");
            future.setDamageAmount(BigDecimal.TEN);
            expectError(cl, new Request(CommandType.REPORT_CLAIM).with("claim", future), "UC7",
                    "Заявление с датой события в будущем", "не может быть в будущем");
            PayoutDecision pd = ag.ok(new Request(CommandType.PREVIEW_PAYOUT).with("claimId", reported.getId())
                    .with("assessedDamage", new BigDecimal("3000")));
            check("UC8", "Расчёт выплаты (ущерб оценён в 3 000)", "Выплата 3 000,00 (франшиза 0)",
                    pd.isApproved() ? "Выплата " + pd.getAmount() : "Отказ: " + pd.getReason(),
                    pd.isApproved() && pd.getAmount().compareTo(new BigDecimal("3000.00")) == 0);
            Claim settled = ag.ok(new Request(CommandType.SETTLE_CLAIM).with("claimId", reported.getId())
                    .with("assessedDamage", new BigDecimal("3000")).with("comment", "Акт осмотра № 15"));
            check("UC8", "Утверждение решения по страховому случаю", "Статус «Выплата произведена»",
                    settled.getStatus().getTitle() + ", " + settled.getPayoutAmount(), "APPROVED".equals(settled.getStatus().name()));
            expectError(ag, new Request(CommandType.SETTLE_CLAIM).with("claimId", reported.getId())
                    .with("assessedDamage", new BigDecimal("3000")), "UC8", "Повторное решение по тому же случаю", "уже принято");
            Claim big = new Claim();
            big.setPolicyId(policy.getId());
            big.setIncidentDate(today);
            big.setDescription("Пожар");
            big.setDamageAmount(new BigDecimal("60000"));
            Claim bigReported = cl.ok(new Request(CommandType.REPORT_CLAIM).with("claim", big));
            Claim exhausted = ag.ok(new Request(CommandType.SETTLE_CLAIM).with("claimId", bigReported.getId())
                    .with("assessedDamage", new BigDecimal("60000")));
            Policy afterBig = null;
            for (Policy p : cl.<List<Policy>>ok(new Request(CommandType.GET_MY_POLICIES))) {
                if (p.getId() == policy.getId()) {
                    afterBig = p;
                }
            }
            check("UC8", "Ущерб больше остатка страховой суммы", "Выплата 47 000,00, договор «Исполнен»",
                    exhausted.getPayoutAmount() + ", договор: " + afterBig.getStatus().getTitle(),
                    exhausted.getPayoutAmount().compareTo(new BigDecimal("47000.00")) == 0
                            && afterBig.getStatus() == PolicyStatus.FULFILLED);

            // ---------- продление и расторжение
            List<Policy> active = ag.ok(new Request(CommandType.GET_POLICIES).with("status", "ACTIVE"));
            Policy farEnd = null;
            for (Policy p : active) {
                if (p.getEndDate().isAfter(today.plusDays(40)) && p.getPayoutsAmount().signum() == 0
                        && p.getPaidAmount().signum() > 0) {
                    farEnd = p;
                    break;
                }
            }
            expectError(ag, new Request(CommandType.RENEW_POLICY).with("policyId", farEnd.getId()), "UC9",
                    "Продление договора, до окончания которого больше 30 дней", "Продлить можно");
            List<Policy> expired = ag.ok(new Request(CommandType.GET_POLICIES).with("status", "EXPIRED"));
            Policy renewed = null;
            String renewMsg = "нет истёкших договоров";
            for (Policy p : expired) {
                Response r = ag.send(new Request(CommandType.RENEW_POLICY).with("policyId", p.getId()));
                if (r.isSuccess()) {
                    renewed = r.getData();
                    renewMsg = "Новый договор " + renewed.getPolicyNumber() + " (" + renewed.getStatus().getTitle() + ")";
                    break;
                }
                renewMsg = r.getMessage();
            }
            check("UC9", "Продление истёкшего договора", "Новый договор «Ожидает оплаты»", renewMsg,
                    renewed != null && renewed.getStatus() == PolicyStatus.WAITING_PAYMENT);
            RefundCalculation rc = ag.ok(new Request(CommandType.PREVIEW_REFUND).with("policyId", farEnd.getId()));
            Policy term = ag.ok(new Request(CommandType.TERMINATE_POLICY).with("policyId", farEnd.getId()));
            check("UC10", "Досрочное расторжение с расчётом возврата", "«Расторгнут», возврат = расчёту",
                    term.getStatus().getTitle() + ", возврат " + term.getRefundAmount() + " (расчёт " + rc.getRefund() + ")",
                    term.getStatus() == PolicyStatus.TERMINATED && term.getRefundAmount().compareTo(rc.getRefund()) == 0);

            // ---------- контроль
            List<ControlItem> control = ag.ok(new Request(CommandType.GET_CONTROL_LIST));
            check("UC11", "Список просроченных взносов и истекающих договоров", "Непустой список",
                    control.size() + " записей", !control.isEmpty());
            Response refresh = ag.send(new Request(CommandType.REFRESH_STATUSES));
            check("UC11", "Пересчёт состояний договоров", "Выполнен", refresh.getMessage(), refresh.isSuccess());

            // ---------- администратор
            ad.login("admin", "admin123");
            InsuranceType nt = new InsuranceType();
            nt.setName("Страхование гаража " + suffix);
            nt.setCategory(InsuranceCategory.PROPERTY);
            nt.setDescription("Гаражи и хозяйственные постройки");
            nt.setBaseRate(new BigDecimal("0.0070"));
            nt.setMinSum(new BigDecimal("2000"));
            nt.setMaxSum(new BigDecimal("50000"));
            nt.setMaxTermMonths(12);
            InsuranceType saved = ad.ok(new Request(CommandType.SAVE_TYPE).with("type", nt));
            check("UC13", "Добавление вида страхования", "Сохранён с новым идентификатором", "id = " + saved.getId(), saved.getId() > 0);
            nt.setBaseRate(new BigDecimal("1.5"));
            expectError(ad, new Request(CommandType.SAVE_TYPE).with("type", nt), "UC13", "Тариф больше 1", "долей страховой суммы");
            Agent agent = new Agent();
            agent.setLastName("Новикова");
            agent.setFirstName("Дарья");
            agent.setPosition("Страховой агент");
            agent.setCommissionRate(new BigDecimal("0.10"));
            Agent created = ad.ok(new Request(CommandType.CREATE_AGENT).with("login", "agent" + suffix)
                    .with("password", "agent123").with("agent", agent));
            check("UC14", "Создание учётной записи агента", "Агент создан", created.getFullName() + ", логин " + created.getLogin(), created.getId() > 0);
            List<User> users = ad.ok(new Request(CommandType.GET_USERS));
            long uid = 0;
            for (User u : users) {
                if (u.getLogin().equals(clientLogin)) {
                    uid = u.getId();
                }
            }
            ad.ok(new Request(CommandType.SET_USER_BLOCKED).with("userId", uid).with("blocked", true));
            try (Conn blocked = new Conn()) {
                expectError(blocked, new Request(CommandType.LOGIN).with("login", clientLogin).with("password", "test123"),
                        "UC14", "Вход заблокированного пользователя", "заблокирована");
            }
            ad.ok(new Request(CommandType.SET_USER_BLOCKED).with("userId", uid).with("blocked", false));
            try (Conn unblocked = new Conn()) {
                User u = unblocked.login(clientLogin, "test123");
                check("UC14", "Вход после разблокировки", "Вход выполнен", "Вход: " + u.getDisplayName(), u != null);
            }
            List<SalesReportRow> sales = ad.ok(new Request(CommandType.SALES_REPORT)
                    .with("from", LocalDate.of(today.getYear(), 1, 1)).with("to", today));
            int cnt = 0;
            for (SalesReportRow r : sales) {
                cnt += r.getPoliciesCount();
            }
            check("UC15", "Отчёт о продажах агентов за год", "Строка по каждому агенту", sales.size() + " агентов, "
                    + cnt + " договоров", sales.size() >= 5 && cnt > 0);
            List<LossRatioRow> loss = ad.ok(new Request(CommandType.LOSS_RATIO_REPORT)
                    .with("from", LocalDate.of(2025, 1, 1)).with("to", today));
            check("UC16", "Анализ убыточности за 2025–2026 гг.", "Строка по каждому виду с коэффициентом",
                    loss.size() + " видов, напр. " + loss.get(0).getTypeName() + ": " + loss.get(0).getLossRatio() + " %",
                    loss.size() >= 4);
            expectError(ad, new Request(CommandType.SALES_REPORT).with("from", today).with("to", today.minusDays(5)),
                    "UC15", "Отчёт с перепутанными датами периода", "Начало периода");
        }

        // ---------- многопоточность
        int n = 20;
        ExecutorService ex = Executors.newFixedThreadPool(n);
        List<Callable<Boolean>> tasks = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            tasks.add(() -> {
                try (Conn c = new Conn()) {
                    c.login("ivanov", "client123");
                    c.ok(new Request(CommandType.GET_MY_POLICIES));
                    c.ok(new Request(CommandType.GET_ACTIVE_TYPES));
                    return true;
                }
            });
        }
        long t0 = System.currentTimeMillis();
        int okCount = 0;
        for (Future<Boolean> f : ex.invokeAll(tasks)) {
            okCount += f.get() ? 1 : 0;
        }
        long ms = System.currentTimeMillis() - t0;
        check("—", n + " клиентов одновременно: вход и запросы", "Все запросы выполнены",
                okCount + " из " + n + " за " + ms + " мс", okCount == n);

        // гонка: два агента одновременно оформляют одну заявку
        Application race = new Application();
        try (Conn c = new Conn()) {
            c.login("novikova", "client123");
            Application ra = new Application();
            ra.setTypeId(4);
            ra.setRequestedSum(new BigDecimal("10000"));
            ra.setTermMonths(12);
            ra.setInstallments(1);
            ra.setObjectDescription("Страхование на время работы");
            race = c.ok(new Request(CommandType.SUBMIT_APPLICATION).with("application", ra));
        }
        final long raceId = race.getId();
        AtomicInteger success = new AtomicInteger();
        List<Callable<Void>> racers = new ArrayList<>();
        for (String login : new String[] {"kovaleva", "petrov"}) {
            racers.add(() -> {
                try (Conn c = new Conn()) {
                    c.login(login, "agent123");
                    if (c.send(new Request(CommandType.APPROVE_APPLICATION).with("applicationId", raceId)
                            .with("startDate", LocalDate.now().plusDays(1))).isSuccess()) {
                        success.incrementAndGet();
                    }
                }
                return null;
            });
        }
        for (Future<Void> f : ex.invokeAll(racers)) {
            f.get();
        }
        ex.shutdown();
        check("UC5", "Два агента одновременно оформляют одну заявку", "Оформлен ровно один договор",
                "Успешных оформлений: " + success.get(), success.get() == 1);

        if (args.length > 0) {
            try (PrintWriter w = new PrintWriter(Files.newBufferedWriter(Paths.get(args[0]), StandardCharsets.UTF_8))) {
                w.println("№\tUC\tДействие\tОжидаемый результат\tФактический результат\tИтог");
                for (String[] r : RESULTS) {
                    w.println(String.join("\t", r));
                }
            }
        }
        System.out.println("ИТОГО проверок: " + RESULTS.size() + ", не пройдено: " + failed);
        System.exit(failed == 0 ? 0 : 1);
    }
}
