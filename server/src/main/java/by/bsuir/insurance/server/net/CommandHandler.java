package by.bsuir.insurance.server.net;

import by.bsuir.insurance.common.protocol.Request;
import by.bsuir.insurance.common.protocol.Response;

/** Обработчик одной команды протокола (паттерн «Команда»). */
@FunctionalInterface
public interface CommandHandler {
    Response handle(Session session, Request request) throws Exception;
}
