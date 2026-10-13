"""Архитектура ПС (раздел 2.2) и схема навигации пользовательского интерфейса (раздел 2.4)."""
import subprocess
import sys
from html import escape
from pathlib import Path

sys.path.insert(0, str(Path(__file__).parent))
from render import render

OUT = Path("/home/claude/kp/diagrams")


def T(x, y, s, size=15, anchor="middle", weight="normal", italic=False):
    st = ' font-style="italic"' if italic else ""
    lines = s.split("\n")
    return "".join(f'<text x="{x}" y="{y + i * (size + 3)}" font-size="{size}" text-anchor="{anchor}" '
                   f'font-weight="{weight}"{st}>{escape(l)}</text>' for i, l in enumerate(lines))


def box(x, y, w, h, title, body="", fill="#fff", bold=True, size=14):
    s = f'<rect x="{x}" y="{y}" width="{w}" height="{h}" rx="4" fill="{fill}" stroke="#000" stroke-width="1.4"/>'
    if body:
        s += T(x + w / 2, y + 20, title, size=size + 1, weight="bold" if bold else "normal")
        s += T(x + w / 2, y + 40, body, size=size - 1)
    else:
        n = title.count("\n") + 1
        s += T(x + w / 2, y + h / 2 - (n - 1) * (size + 3) / 2 + size * 0.35, title, size=size,
               weight="bold" if bold else "normal")
    return s


def arrow(pts, both=False, dash=False, label=None, lpos=None, anchor="middle"):
    d = " ".join(f"{x},{y}" for x, y in pts)
    extra = ' marker-start="url(#a2)"' if both else ""
    da = ' stroke-dasharray="6,4"' if dash else ""
    s = f'<polyline points="{d}" fill="none" stroke="#000" stroke-width="1.5" marker-end="url(#a1)"{extra}{da}/>'
    if label:
        s += T(lpos[0], lpos[1], label, size=13, anchor=anchor)
    return s


DEFS = ('<defs><marker id="a1" viewBox="0 0 10 10" refX="10" refY="5" markerWidth="10" markerHeight="10" orient="auto" '
        'markerUnits="userSpaceOnUse"><path d="M0,1.5 L10,5 L0,8.5 z" fill="#000"/></marker>'
        '<marker id="a2" viewBox="0 0 10 10" refX="0" refY="5" markerWidth="10" markerHeight="10" orient="auto" '
        'markerUnits="userSpaceOnUse"><path d="M10,1.5 L0,5 L10,8.5 z" fill="#000"/></marker></defs>')


def architecture():
    W, H = 1500, 860
    g = [f'<svg xmlns="http://www.w3.org/2000/svg" width="{W}" height="{H}" viewBox="0 0 {W} {H}">', DEFS,
         f'<rect width="{W}" height="{H}" fill="#fff"/>']
    # уровни
    tiers = [(20, "Уровень представления\n(клиентское приложение, JVM клиента)", 360),
             (420, "Уровень бизнес-логики\n(сервер приложений, JVM сервера)", 760),
             (1220, "Уровень данных\n(СУБД)", 260)]
    for x, title, w in tiers:
        g.append(f'<rect x="{x}" y="20" width="{w}" height="{H - 40}" rx="8" fill="#F4F6F8" stroke="#555" stroke-width="1.2" stroke-dasharray="8,5"/>')
        g.append(T(x + w / 2, 46, title, size=15, weight="bold"))
    # клиент
    g.append(box(50, 110, 300, 150, "Окна и панели Swing",
                 "LoginFrame, RegisterDialog\nClientFrame, AgentFrame, AdminFrame\n(наследники BaseFrame)\nпанели вкладок, таблицы, диаграммы", fill="#fff"))
    g.append(box(50, 310, 300, 90, "Ui", "вызов команд, сообщения,\nобработка ошибок ввода"))
    g.append(box(50, 450, 300, 110, "ServerConnection", "«Одиночка»: сокет,\nObjectOutputStream / ObjectInputStream,\nповтор при обрыве"))
    g.append(box(50, 620, 300, 80, "common (общий модуль)", "модель, Request / Response,\nCommandType, исключения"))
    g.append(arrow([(200, 260), (200, 310)]))
    g.append(arrow([(200, 400), (200, 450)]))
    # сеть
    g.append(arrow([(350, 505), (460, 505)], both=True, label="TCP/IP, порт 5555", lpos=(405, 492)))
    g.append(T(405, 528, "Request / Response\n(сериализация)", size=12))
    # сервер
    g.append(box(460, 100, 320, 100, "ServerApp", "ServerSocket.accept();\nнастройки ServerConfig\n(server.properties, аргументы)"))
    g.append(box(460, 250, 320, 100, "ExecutorService", "пул из N потоков (server.threads);\nподключение → задача пула"))
    g.append(box(460, 400, 320, 150, "ClientHandler × N", "по потоку на подключение:\nчтение Request, сеанс Session,\nотправка Response; перевод\nисключений в сообщения"))
    g.append(box(840, 100, 310, 110, "CommandDispatcher", "«Команда»: CommandType →\nCommandHandler; проверка\nавторизации и роли"))
    g.append(box(840, 250, 310, 150, "Сервисы", "AuthService, ApplicationService,\nPolicyService, ClaimService,\nAdminService, ReportService;\nграницы транзакций"))
    g.append(box(840, 440, 310, 130, "Бизнес-логика (logic)", "PremiumCalculator + стратегии,\nPayoutCalculator, RefundCalculator,\nInstallmentScheduler,\nPolicyStatusRules"))
    g.append(box(840, 610, 310, 90, "DAO (dao)", "AbstractDao и 9 наследников,\nSQL через PreparedStatement"))
    g.append(box(840, 735, 310, 90, "ConnectionManager", "«Одиночка»: пул соединений JDBC,\ninTransaction / withConnection"))
    g.append(box(460, 640, 320, 100, "Планировщик", "ScheduledExecutorService:\nпересчёт состояний договоров\nкаждые 6 ч"))
    g.append(arrow([(620, 200), (620, 250)]))
    g.append(arrow([(620, 350), (620, 400)]))
    g.append(arrow([(780, 470), (810, 470), (810, 155), (840, 155)]))
    g.append(arrow([(995, 210), (995, 250)]))
    g.append(arrow([(995, 400), (995, 440)]))
    g.append(arrow([(1120, 400), (1120, 610)]))
    g.append(arrow([(995, 570), (995, 610)]))
    g.append(arrow([(995, 700), (995, 735)]))
    g.append(arrow([(780, 690), (815, 690), (815, 340), (840, 340)], dash=True))
    # БД
    g.append(f'<path d="M1260,430 a90,22 0 0,0 180,0 a90,22 0 0,0 -180,0 v170 a90,22 0 0,0 180,0 v-170" fill="#fff" stroke="#000" stroke-width="1.4"/>')
    g.append(T(1350, 490, "PostgreSQL 16", size=15, weight="bold"))
    g.append(T(1350, 515, "БД insurance:\n9 таблиц (3НФ),\nвнешние ключи,\nограничения CHECK", size=13))
    g.append(arrow([(1150, 780), (1200, 780), (1200, 560), (1260, 560)], both=True, label="JDBC,\nпорт 5432", lpos=(1240, 640)))
    g.append("</svg>")
    svg = "".join(g)
    (OUT / "architecture.svg").write_text(svg, encoding="utf8")
    render(svg, W, H, png=str(OUT / "architecture.png"), scale=2)


def navigation():
    dot = r'''digraph G {
graph [rankdir=TB, nodesep=0.45, ranksep=0.6, fontname="Liberation Serif", pad=0.2];
node [shape=box, style="rounded", fontname="Liberation Serif", fontsize=13, penwidth=1.3];
edge [fontname="Liberation Serif", fontsize=11, penwidth=1.2, arrowsize=0.8];
login [label="Окно входа\nLoginFrame", style="rounded,bold"];
reg [label="Регистрация клиента\nRegisterDialog (модальный)"];
cf [shape=record, style="rounded", label="{Личный кабинет клиента\nClientFrame|Расчёт и заявка|Мои заявки|Мои договоры и взносы|Страховые случаи}"];
af [shape=record, style="rounded", label="{Рабочее место агента\nAgentFrame|Заявки|Договоры|Страховые случаи|Контроль}"];
adm [shape=record, style="rounded", label="{Администрирование\nAdminFrame|Виды страхования|Агенты|Пользователи|Продажи агентов|Убыточность}"];
dlg [label="Стандартные диалоги:\nподтверждение «Да / Нет»,\nсообщение об успехе,\nсообщение об ошибке,\nввод причины отказа,\nсохранение файла отчёта"];
login -> reg [label="«Регистрация\nклиента»"];
reg -> cf [label="успешная\nрегистрация"];
login -> cf [label="роль «Клиент»"];
login -> af [label="роль «Агент»"];
login -> adm [label="роль\n«Администратор»"];
cf -> dlg [style=dashed]; af -> dlg [style=dashed]; adm -> dlg [style=dashed];
cf -> login [label="«Выйти»", style=dotted, constraint=false];
}'''
    (OUT / "navigation.dot").write_text(dot, encoding="utf8")
    subprocess.run(["dot", "-Tpng", "-Gdpi=170", str(OUT / "navigation.dot"), "-o", str(OUT / "navigation.png")], check=True)


if __name__ == "__main__":
    architecture()
    navigation()
    print("ok")
