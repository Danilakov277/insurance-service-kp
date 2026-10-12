package by.bsuir.insurance.common.protocol;

import by.bsuir.insurance.common.model.Role;

import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

/**
 * Команды протокола клиент-сервер. Для каждой команды задаются роли,
 * которым она доступна; пустой набор означает доступ без авторизации.
 */
public enum CommandType {
    // общие
    LOGIN(),
    REGISTER_CLIENT(),
    LOGOUT(Role.CLIENT, Role.AGENT, Role.ADMIN),
    GET_ACTIVE_TYPES(Role.CLIENT, Role.AGENT, Role.ADMIN),
    CALCULATE_PREMIUM(Role.CLIENT, Role.AGENT),
    GET_PAYMENTS(Role.CLIENT, Role.AGENT, Role.ADMIN),
    // клиент
    SUBMIT_APPLICATION(Role.CLIENT),
    GET_MY_APPLICATIONS(Role.CLIENT),
    GET_MY_POLICIES(Role.CLIENT),
    PAY_INSTALLMENT(Role.CLIENT),
    REPORT_CLAIM(Role.CLIENT),
    GET_MY_CLAIMS(Role.CLIENT),
    GET_MY_PROFILE(Role.CLIENT),
    // агент
    GET_NEW_APPLICATIONS(Role.AGENT),
    APPROVE_APPLICATION(Role.AGENT),
    REJECT_APPLICATION(Role.AGENT),
    GET_POLICIES(Role.AGENT, Role.ADMIN),
    RENEW_POLICY(Role.AGENT),
    PREVIEW_REFUND(Role.AGENT),
    TERMINATE_POLICY(Role.AGENT),
    GET_PENDING_CLAIMS(Role.AGENT),
    GET_ALL_CLAIMS(Role.AGENT, Role.ADMIN),
    PREVIEW_PAYOUT(Role.AGENT),
    SETTLE_CLAIM(Role.AGENT),
    GET_CONTROL_LIST(Role.AGENT),
    REFRESH_STATUSES(Role.AGENT, Role.ADMIN),
    // администратор
    GET_ALL_TYPES(Role.ADMIN),
    SAVE_TYPE(Role.ADMIN),
    GET_AGENTS(Role.ADMIN),
    CREATE_AGENT(Role.ADMIN),
    GET_USERS(Role.ADMIN),
    SET_USER_BLOCKED(Role.ADMIN),
    SALES_REPORT(Role.ADMIN),
    LOSS_RATIO_REPORT(Role.ADMIN);

    private final Set<Role> roles;

    CommandType(Role... roles) {
        this.roles = roles.length == 0
                ? Collections.<Role>emptySet()
                : Collections.unmodifiableSet(EnumSet.copyOf(Arrays.asList(roles)));
    }

    /** Требует ли команда авторизации. */
    public boolean requiresAuth() {
        return !roles.isEmpty();
    }

    public boolean isAllowedFor(Role role) {
        return roles.contains(role);
    }
}
