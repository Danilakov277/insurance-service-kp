package by.bsuir.insurance.server.net;

import by.bsuir.insurance.common.exception.AuthException;
import by.bsuir.insurance.common.exception.ValidationException;
import by.bsuir.insurance.common.model.Agent;
import by.bsuir.insurance.common.model.Application;
import by.bsuir.insurance.common.model.Claim;
import by.bsuir.insurance.common.model.Client;
import by.bsuir.insurance.common.model.InsuranceType;
import by.bsuir.insurance.common.model.PolicyStatus;
import by.bsuir.insurance.common.model.User;
import by.bsuir.insurance.common.protocol.CommandType;
import by.bsuir.insurance.common.protocol.Request;
import by.bsuir.insurance.common.protocol.Response;
import by.bsuir.insurance.server.service.AdminService;
import by.bsuir.insurance.server.service.ApplicationService;
import by.bsuir.insurance.server.service.AuthService;
import by.bsuir.insurance.server.service.ClaimService;
import by.bsuir.insurance.server.service.PolicyService;
import by.bsuir.insurance.server.service.ReportService;

import java.time.LocalDate;
import java.util.EnumMap;
import java.util.Map;

/**
 * Диспетчер команд: сопоставляет тип команды обработчику и проверяет права доступа.
 * Один экземпляр используется всеми потоками, поэтому обработчики не хранят состояния.
 */
public class CommandDispatcher {
    private final Map<CommandType, CommandHandler> handlers = new EnumMap<>(CommandType.class);

    public CommandDispatcher() {
        AuthService auth = new AuthService();
        AdminService admin = new AdminService();
        ApplicationService apps = new ApplicationService();
        PolicyService policies = new PolicyService();
        ClaimService claims = new ClaimService();
        ReportService reports = new ReportService();

        // ---- общие
        handlers.put(CommandType.LOGIN, (s, r) -> {
            User u = auth.login(r.getString("login"), r.getString("password"));
            s.setUser(u);
            return Response.ok("Добро пожаловать, " + u.getDisplayName(), u);
        });
        handlers.put(CommandType.LOGOUT, (s, r) -> {
            s.setUser(null);
            return Response.ok(null);
        });
        handlers.put(CommandType.REGISTER_CLIENT, (s, r) -> {
            Client c = (Client) r.get("client");
            User u = auth.registerClient(r.getString("login"), r.getString("password"), c);
            s.setUser(u);
            return Response.ok("Регистрация выполнена", u);
        });
        handlers.put(CommandType.GET_ACTIVE_TYPES, (s, r) -> Response.ok(admin.activeTypes()));
        handlers.put(CommandType.CALCULATE_PREMIUM, (s, r) -> {
            Application a = (Application) r.get("application");
            long clientId = r.has("clientId") ? r.getLong("clientId") : s.personId();
            LocalDate start = r.has("startDate") ? r.getDate("startDate") : LocalDate.now().plusDays(1);
            return Response.ok(apps.quote(clientId, a, start));
        });
        handlers.put(CommandType.GET_PAYMENTS, (s, r) -> Response.ok(policies.payments(s.getUser(), r.getLong("policyId"))));

        // ---- клиент
        handlers.put(CommandType.SUBMIT_APPLICATION, (s, r) ->
                Response.ok("Заявка отправлена на рассмотрение", apps.submit(s.personId(), (Application) r.get("application"))));
        handlers.put(CommandType.GET_MY_APPLICATIONS, (s, r) -> Response.ok(apps.listByClient(s.personId())));
        handlers.put(CommandType.GET_MY_POLICIES, (s, r) -> Response.ok(policies.listByClient(s.personId())));
        handlers.put(CommandType.PAY_INSTALLMENT, (s, r) ->
                Response.ok("Взнос внесён", policies.payInstallment(s.getUser(), r.getLong("policyId"))));
        handlers.put(CommandType.REPORT_CLAIM, (s, r) ->
                Response.ok("Заявление зарегистрировано", claims.report(s.getUser(), (Claim) r.get("claim"))));
        handlers.put(CommandType.GET_MY_CLAIMS, (s, r) -> Response.ok(claims.listByClient(s.personId())));
        handlers.put(CommandType.GET_MY_PROFILE, (s, r) -> Response.ok(auth.profile(s.personId())));

        // ---- агент
        handlers.put(CommandType.GET_NEW_APPLICATIONS, (s, r) -> Response.ok(apps.listNew()));
        handlers.put(CommandType.APPROVE_APPLICATION, (s, r) ->
                Response.ok("Договор оформлен", apps.approve(s.personId(), r.getLong("applicationId"), r.getDate("startDate"))));
        handlers.put(CommandType.REJECT_APPLICATION, (s, r) ->
                Response.ok("Заявка отклонена", apps.reject(s.personId(), r.getLong("applicationId"), r.getString("comment"))));
        handlers.put(CommandType.GET_POLICIES, (s, r) -> Response.ok(policies.listAll(
                r.has("status") ? PolicyStatus.valueOf(r.getString("status")) : null)));
        handlers.put(CommandType.RENEW_POLICY, (s, r) ->
                Response.ok("Договор продлён", policies.renew(s.personId(), r.getLong("policyId"))));
        handlers.put(CommandType.PREVIEW_REFUND, (s, r) -> Response.ok(policies.previewRefund(r.getLong("policyId"), LocalDate.now())));
        handlers.put(CommandType.TERMINATE_POLICY, (s, r) ->
                Response.ok("Договор расторгнут", policies.terminate(r.getLong("policyId"))));
        handlers.put(CommandType.GET_PENDING_CLAIMS, (s, r) -> Response.ok(claims.listPending()));
        handlers.put(CommandType.GET_ALL_CLAIMS, (s, r) -> Response.ok(claims.listAll()));
        handlers.put(CommandType.PREVIEW_PAYOUT, (s, r) ->
                Response.ok(claims.preview(r.getLong("claimId"), r.getDecimal("assessedDamage"))));
        handlers.put(CommandType.SETTLE_CLAIM, (s, r) -> Response.ok("Решение по страховому случаю принято",
                claims.settle(s.personId(), r.getLong("claimId"), r.getDecimal("assessedDamage"), r.getString("comment"))));
        handlers.put(CommandType.GET_CONTROL_LIST, (s, r) -> Response.ok(policies.controlList()));
        handlers.put(CommandType.REFRESH_STATUSES, (s, r) -> {
            int n = policies.refreshStatuses();
            return Response.ok("Состояния договоров пересчитаны, изменено: " + n, n);
        });

        // ---- администратор
        handlers.put(CommandType.GET_ALL_TYPES, (s, r) -> Response.ok(admin.allTypes()));
        handlers.put(CommandType.SAVE_TYPE, (s, r) -> Response.ok("Вид страхования сохранён", admin.saveType((InsuranceType) r.get("type"))));
        handlers.put(CommandType.GET_AGENTS, (s, r) -> Response.ok(admin.agents()));
        handlers.put(CommandType.CREATE_AGENT, (s, r) -> Response.ok("Учётная запись агента создана",
                admin.createAgent(r.getString("login"), r.getString("password"), (Agent) r.get("agent"))));
        handlers.put(CommandType.GET_USERS, (s, r) -> Response.ok(admin.users()));
        handlers.put(CommandType.SET_USER_BLOCKED, (s, r) -> Response.ok(r.getBoolean("blocked") ? "Пользователь заблокирован"
                : "Пользователь разблокирован", admin.setBlocked(s.getUser(), r.getLong("userId"), r.getBoolean("blocked"))));
        handlers.put(CommandType.SALES_REPORT, (s, r) -> Response.ok(reports.sales(r.getDate("from"), r.getDate("to"))));
        handlers.put(CommandType.LOSS_RATIO_REPORT, (s, r) -> Response.ok(reports.lossRatio(r.getDate("from"), r.getDate("to"))));
    }

    /** Проверка прав и выполнение команды. */
    public Response dispatch(Session session, Request request) throws Exception {
        CommandType type = request.getCommand();
        CommandHandler handler = handlers.get(type);
        if (handler == null) {
            throw new ValidationException("Неизвестная команда: " + type);
        }
        if (type.requiresAuth()) {
            if (!session.isAuthenticated()) {
                throw new AuthException("Требуется авторизация");
            }
            if (!type.isAllowedFor(session.getUser().getRole())) {
                throw new AuthException("Недостаточно прав для выполнения операции");
            }
        }
        return handler.handle(session, request);
    }
}
