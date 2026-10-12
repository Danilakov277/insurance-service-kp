"""Генератор демонстрационных данных (database/02_demo_data.sql).

Расчёт премий, графиков взносов, выплат и статусов повторяет алгоритмы сервера,
поэтому демо-данные согласованы с программой. Дата «сегодня» фиксирована: TODAY.
"""
import hashlib
import random
from datetime import date, timedelta, datetime
from decimal import Decimal, ROUND_HALF_UP
from pathlib import Path

random.seed(414351)
TODAY = date(2026, 10, 13)
OUT = Path(__file__).resolve().parent.parent / "database" / "02_demo_data.sql"

D = Decimal
def r2(x): return x.quantize(D("0.01"), rounding=ROUND_HALF_UP)


def pwd(password):
    salt = hashlib.md5(("salt:" + password + str(random.random())).encode()).hexdigest()
    return hashlib.sha256((salt + password).encode("utf-8")).hexdigest(), salt


def add_months(d, m):
    y, mo = divmod(d.month - 1 + m, 12)
    y += d.year; mo += 1
    import calendar
    day = min(d.day, calendar.monthrange(y, mo)[1])
    return date(y, mo, day)


TYPES = [
    # name, category, description, rate, min, max, max_term, active
    ("Страхование квартиры", "PROPERTY", "Конструктивные элементы, отделка и инженерное оборудование квартиры от пожара, залива, стихийных бедствий и противоправных действий третьих лиц", "0.0050", 5000, 300000, 36, True),
    ("Страхование домашнего имущества", "PROPERTY", "Мебель, бытовая техника и другое движимое имущество в жилом помещении", "0.0080", 1000, 100000, 24, True),
    ("КАСКО", "CASCO", "Наземные транспортные средства от ущерба, хищения и угона", "0.0450", 5000, 200000, 12, True),
    ("Страхование от несчастных случаев", "ACCIDENT", "Травмы, инвалидность и смерть в результате несчастного случая", "0.0120", 1000, 100000, 24, True),
    ("Медицинские расходы выезжающих за рубеж", "TRAVEL", "Оплата экстренной медицинской помощи во время пребывания за границей", "0.0300", 10000, 100000, 12, True),
    ("Страхование садового домика", "PROPERTY", "Строения на садовых участках (приём новых договоров приостановлен)", "0.0090", 3000, 80000, 12, False),
]

AGENTS = [
    ("kovaleva", "Ковалёва", "Анна", "Сергеевна", "Ведущий страховой агент", date(2019, 3, 1), "0.1200"),
    ("petrov", "Петров", "Дмитрий", "Игоревич", "Страховой агент", date(2021, 9, 15), "0.1000"),
    ("sidorovich", "Сидорович", "Елена", "Викторовна", "Страховой агент", date(2022, 2, 7), "0.1000"),
    ("zhuk", "Жук", "Максим", "Андреевич", "Стажёр", date(2025, 11, 3), "0.0800"),
]

CLIENTS = [
    ("ivanov", "Иванов", "Сергей", "Петрович", date(1985, 4, 12), "г. Минск, ул. Притыцкого, 34-56"),
    ("novikova", "Новикова", "Ольга", "Александровна", date(1992, 11, 3), "г. Минск, пр-т Независимости, 120-14"),
    ("kozlov", "Козлов", "Андрей", "Николаевич", date(1958, 1, 27), "г. Минск, ул. Сурганова, 17-3"),
    ("morozova", "Морозова", "Татьяна", "Ивановна", date(1963, 7, 9), "г. Борисов, ул. Чапаева, 8-21"),
    ("volkov", "Волков", "Павел", "Юрьевич", date(2003, 5, 30), "г. Минск, ул. Есенина, 61-102"),
    ("lebedeva", "Лебедева", "Мария", "Олеговна", date(1998, 2, 14), "г. Минск, ул. Кальварийская, 25-77"),
    ("sokolov", "Соколов", "Игорь", "Владимирович", date(1979, 9, 2), "г. Молодечно, ул. Великий Гостинец, 41-5"),
    ("kuznetsova", "Кузнецова", "Наталья", "Сергеевна", date(1988, 12, 19), "г. Минск, ул. Якуба Коласа, 11-40"),
    ("pavlovich", "Павлович", "Виктор", "Степанович", date(1955, 3, 5), "г. Минск, ул. Воронянского, 7-12"),
    ("grigoryeva", "Григорьева", "Анастасия", "Дмитриевна", date(2004, 8, 22), "г. Минск, ул. Беды, 2-301"),
    ("zaitsev", "Зайцев", "Николай", "Алексеевич", date(1970, 10, 1), "г. Заславль, ул. Советская, 99"),
    ("bondarenko", "Бондаренко", "Юлия", "Викторовна", date(1995, 6, 11), "г. Минск, ул. Матусевича, 33-9"),
    ("romanov", "Романов", "Алексей", "Геннадьевич", date(1983, 1, 16), "г. Минск, ул. Ложинская, 14-88"),
    ("shevchenko", "Шевченко", "Ирина", "Петровна", date(1967, 4, 28), "г. Минск, ул. Ангарская, 52-17"),
    ("belov", "Белов", "Денис", "Олегович", date(1990, 12, 7), "г. Дзержинск, ул. Минская, 3-4"),
]

DESCR = {
    "PROPERTY": ["Квартира, 2 комнаты, 54 м², кирпичный дом 1998 г. постройки",
                 "Квартира, 3 комнаты, 78 м², панельный дом, 5-й этаж",
                 "Квартира, 1 комната, 38 м², новостройка, отделка 2024 г.",
                 "Деревянный дом с мансардой, 96 м²"],
    "CASCO": ["Volkswagen Polo, 2019 г.в., госномер 1234 AB-7",
              "Toyota RAV4, 2021 г.в., госномер 5678 KE-7",
              "Geely Coolray, 2023 г.в., госномер 9012 MP-5",
              "Renault Logan, 2016 г.в., госномер 3456 HO-7"],
    "ACCIDENT": ["Страхование на время работы и в быту",
                 "Активные занятия спортом (футбол, бег)",
                 "Страхование на период отпуска"],
    "TRAVEL": ["Поездка в Польшу, туризм",
               "Горнолыжный отдых в Грузии",
               "Деловая поездка в Германию"],
}

KEYWORD_FACTORS = {"PROPERTY": (("деревян",), D("1.20")), "ACCIDENT": (("спорт",), D("1.30")),
                   "TRAVEL": (("горнолыж", "спорт"), D("1.50"))}


def age_on(bd, d):
    return d.year - bd.year - ((d.month, d.day) < (bd.month, bd.day))


def premium(t, s, term, bd, start, claims_2y, fr_pct, descr):
    rate = D(t[3])
    age = age_on(bd, start)
    kv = D("1.20") if age < 25 else (D("1.35") if age > 60 else D("1.00"))
    ks = D(term) / D(12)
    if term < 12:
        ks = ks * D("1.10")
    kbm = D("0.90") if claims_2y == 0 else (D("1.00") if claims_2y == 1 else D("1.25"))
    kf = {0: D("1.00"), 1: D("0.95"), 2: D("0.90"), 5: D("0.80")}[fr_pct]
    cat = t[1]
    ktype = D("1.00")
    if cat == "PROPERTY" and s > 100000:
        ktype = D("0.95")
    if cat == "CASCO" and age < 25:
        ktype = D("1.15")
    if cat in KEYWORD_FACTORS:
        kws, f = KEYWORD_FACTORS[cat]
        if any(k in descr.lower() for k in kws):
            ktype = ktype * f
    return r2(D(s) * rate * kv * ks * kbm * kf * ktype)


def schedule(prem, n, start, term):
    step = term // n
    base = (prem / n).quantize(D("0.01"), rounding="ROUND_DOWN")
    out = []
    for i in range(1, n + 1):
        amt = base if i < n else prem - base * (n - 1)
        out.append([i, amt, add_months(start, step * (i - 1)), None])
    return out


lines = ["-- Демонстрационные данные ПС «Страховой сервис» (сгенерированы tools/gen_demo_data.py)",
         "-- Пароли: admin/admin123, агенты — agent123, клиенты — client123", "\\connect insurance", "BEGIN;"]
E = lines.append
def q(v):
    if v is None: return "NULL"
    if isinstance(v, bool): return "TRUE" if v else "FALSE"
    if isinstance(v, (int, Decimal)): return str(v)
    if isinstance(v, (date, datetime)): return f"'{v.isoformat(sep=' ') if isinstance(v, datetime) else v.isoformat()}'"
    return "'" + str(v).replace("'", "''") + "'"

def ins(table, cols, vals):
    E(f"INSERT INTO {table} ({', '.join(cols)}) VALUES ({', '.join(q(v) for v in vals)});")

uid = 0
h, s = pwd("admin123"); uid += 1
ins("users", ["login", "password_hash", "salt", "role_id", "created_at"], ["admin", h, s, 3, datetime(2024, 12, 1, 9, 0)])
for i, t in enumerate(TYPES, 1):
    ins("insurance_types", ["name", "category", "description", "base_rate", "min_sum", "max_sum", "max_term_months", "is_active"],
        [t[0], t[1], t[2], D(t[3]), D(t[4]), D(t[5]), t[6], t[7]])
for i, a in enumerate(AGENTS, 1):
    h, s = pwd("agent123"); uid += 1
    ins("users", ["login", "password_hash", "salt", "role_id", "created_at"], [a[0], h, s, 2, datetime(a[5].year, a[5].month, a[5].day, 10, 0)])
    ins("agents", ["user_id", "last_name", "first_name", "patronymic", "position", "hire_date", "commission_rate"],
        [uid, a[1], a[2], a[3], a[4], a[5], D(a[6])])
clients = []
for i, c in enumerate(CLIENTS, 1):
    h, s = pwd("client123"); uid += 1
    reg = datetime(2024, 12, 10) + timedelta(days=random.randint(0, 200))
    ins("users", ["login", "password_hash", "salt", "role_id", "created_at"], [c[0], h, s, 1, reg])
    passport = random.choice(["MP", "HB", "KH", "MC", "AB"]) + str(random.randint(1000000, 9999999))
    phone = f"+375 {random.choice(['29', '33', '44', '25'])} {random.randint(100, 999)}-{random.randint(10, 99)}-{random.randint(10, 99)}"
    ins("clients", ["user_id", "last_name", "first_name", "patronymic", "birth_date", "passport_no", "phone", "address"],
        [uid, c[1], c[2], c[3], c[4], passport, phone, c[5]])
    clients.append({"id": i, "bd": c[4], "reg": reg, "claims": []})

app_id = 0; pol_id = 0; claim_rows = []
policies = []
agent_weights = [0.34, 0.28, 0.26, 0.12]


def terms_for(cat):
    return {"PROPERTY": [12, 12, 24], "CASCO": [12], "ACCIDENT": [12, 6], "TRAVEL": [1, 3, 12]}[cat]


def inst_for(term):
    opts = [n for n in (1, 2, 4, 12) if n <= term and term % n == 0]
    return random.choice(opts)


start0 = date(2025, 1, 10)
span = (TODAY - start0).days
for k in range(58):
    cl = random.choice(clients)
    ti = random.choices(range(5), weights=[30, 15, 25, 15, 15])[0]
    t = TYPES[ti]
    cat = t[1]
    term = random.choice(terms_for(cat))
    lo, hi = t[4], t[5]
    s = random.randrange(lo, min(hi, lo * 12) + 1, 500 if lo < 5000 else 1000)
    if cat == "TRAVEL": s = random.choice([10000, 20000, 30000, 50000])
    fr = random.choice([0, 0, 1, 2, 5]) if cat in ("PROPERTY", "CASCO") else 0
    n = inst_for(term)
    start = start0 + timedelta(days=int(span * (k / 58) + random.randint(0, 12)))
    if start > TODAY + timedelta(days=5): start = TODAY + timedelta(days=random.randint(1, 5))
    created = datetime.combine(start - timedelta(days=random.randint(1, 6)), datetime.min.time()) + timedelta(hours=random.randint(9, 17))
    if created.date() < cl["reg"].date():
        created = cl["reg"] + timedelta(days=1)
        start = created.date() + timedelta(days=2)
    if start > TODAY + timedelta(days=5) or created.date() > TODAY:
        continue
    descr = random.choice(DESCR[cat])
    claims_2y = sum(1 for (d, paid) in cl["claims"] if paid > 0 and d >= created.date() - timedelta(days=730) and d <= created.date())
    prem = premium(t, s, term, cl["bd"], start, claims_2y, fr, descr)
    agent = random.choices(range(1, 5), weights=agent_weights)[0]
    if AGENTS[agent - 1][5] > created.date(): agent = 1
    app_id += 1; pol_id += 1
    ins("applications", ["client_id", "type_id", "agent_id", "requested_sum", "term_months", "franchise_percent", "installments",
                         "object_description", "status", "decision_comment", "created_at", "decided_at"],
        [cl["id"], ti + 1, agent, D(s), term, fr, n, descr, "APPROVED", "Заявка одобрена, договор оформлен", created - timedelta(hours=3), created])
    end = add_months(start, term) - timedelta(days=1)
    sched = schedule(prem, n, start, term)
    late_payer = random.random() < 0.3 and n > 1
    for row in sched:
        due = row[2]
        if due <= TODAY:
            if row[0] == 1:
                row[3] = min(due, created.date() + timedelta(days=random.randint(0, 3)))
                if row[3] < created.date(): row[3] = created.date()
            elif late_payer and due > TODAY - timedelta(days=60):
                row[3] = None
            else:
                row[3] = due - timedelta(days=random.randint(0, 6))
    if start > TODAY and random.random() < 0.6:
        sched[0][3] = None
    p = {"id": pol_id, "num": f"INS-{created.year}-{pol_id:06d}", "app": app_id, "client": cl, "agent": agent, "type": ti + 1,
         "cat": cat, "sum": D(s), "prem": prem, "fr": r2(D(s) * D(fr) / 100), "term": term, "start": start, "end": end, "created": created,
         "sched": sched, "status": None, "term_at": None, "refund": None, "payouts": D(0)}
    # early termination
    if random.random() < 0.06 and start < TODAY - timedelta(days=60):
        tdate = start + timedelta(days=random.randint(30, min(200, (min(end, TODAY) - start).days)))
        p["term_at"] = datetime.combine(tdate, datetime.min.time()) + timedelta(hours=11)
    policies.append(p)

# two most recent policies are not paid yet (status «Ожидает оплаты»)
for p in policies[-2:]:
    p["created"] = datetime.combine(TODAY - timedelta(days=2), datetime.min.time()) + timedelta(hours=12)
    p["start"] = TODAY + timedelta(days=1)
    p["end"] = add_months(p["start"], p["term"]) - timedelta(days=1)
    p["sched"] = schedule(p["prem"], len(p["sched"]), p["start"], p["term"])
    p["num"] = f"INS-{p['created'].year}-{p['id']:06d}"

# claims
for p in policies:
    if p["start"] >= TODAY - timedelta(days=20) or random.random() > 0.22:
        continue
    last = min(p["end"], TODAY - timedelta(days=2), (p["term_at"].date() if p["term_at"] else p["end"]))
    if last <= p["start"]:
        continue
    inc = p["start"] + timedelta(days=random.randint(5, max(6, (last - p["start"]).days)))
    if inc > last: inc = last
    late = random.random() < 0.08
    rep = inc + timedelta(days=random.randint(35, 50) if late else random.randint(0, 9))
    if rep > TODAY: rep = TODAY
    dmg = r2(p["prem"] * D(str(round(random.uniform(1.2, 4.5), 2))) + p["fr"])
    pending = rep >= TODAY - timedelta(days=12) and random.random() < 0.7
    claim_rows.append({"p": p, "inc": inc, "rep": rep, "dmg": dmg, "pending": pending})

# add a few guaranteed pending claims for the agent demo
for p in sorted(policies, key=lambda x: x["start"], reverse=True):
    if sum(1 for c in claim_rows if c["pending"]) >= 4: break
    if p["term_at"] or p["start"] > TODAY - timedelta(days=40) or any(c["p"] is p for c in claim_rows): continue
    inc = TODAY - timedelta(days=random.randint(3, 9))
    claim_rows.append({"p": p, "inc": inc, "rep": inc + timedelta(days=1), "dmg": r2(p["prem"] * D("2.5") + p["fr"]), "pending": True})

CLAIM_TEXT = {"PROPERTY": ["Залив квартиры соседями сверху", "Короткое замыкание, повреждена проводка и отделка кухни", "Повреждение окон градом"],
              "CASCO": ["ДТП на парковке, повреждён задний бампер", "Повреждение лобового стекла камнем", "Столкновение на перекрёстке"],
              "ACCIDENT": ["Перелом лучевой кости при падении", "Растяжение связок голеностопа"],
              "TRAVEL": ["Обращение в клинику с острым отравлением", "Травма на горнолыжном склоне"]}


def overdue_on(p, d):
    return any(row[2] < d and (row[3] is None or row[3] > d) for row in p["sched"])


claim_rows.sort(key=lambda c: c["rep"])
for c in claim_rows:
    p = c["p"]
    agent = p["agent"]
    c["descr"] = random.choice(CLAIM_TEXT[p["cat"]])
    if c["pending"]:
        c.update(status="REPORTED", payout=D("0.00"), comment=None, dec=None, assessed=None)
        continue
    assessed = c["dmg"] if random.random() < 0.7 else r2(c["dmg"] * D("0.85"))
    dec = datetime.combine(c["rep"] + timedelta(days=random.randint(2, 7)), datetime.min.time()) + timedelta(hours=14)
    if dec.date() > TODAY: dec = datetime.combine(TODAY, datetime.min.time()) + timedelta(hours=10)
    remaining = p["sum"] - p["payouts"]
    if not (p["start"] <= c["inc"] <= p["end"]):
        st, pay, com = "REJECTED", D("0.00"), "Событие произошло вне периода действия договора"
    elif overdue_on(p, c["inc"]):
        st, pay, com = "REJECTED", D("0.00"), "На дату события имелись просроченные страховые взносы"
    elif (c["rep"] - c["inc"]).days > 30:
        st, pay, com = "REJECTED", D("0.00"), "Заявление подано позднее 30 дней с даты события"
    elif assessed <= p["fr"]:
        st, pay, com = "REJECTED", D("0.00"), "Размер ущерба не превышает франшизу"
    else:
        pay = min(assessed - p["fr"], remaining)
        st, com = "APPROVED", "Выплата произведена по акту осмотра"
        p["payouts"] += pay
        c["p"]["client"]["claims"].append((c["inc"], pay))
    c.update(status=st, payout=r2(pay), comment=com, dec=dec, assessed=assessed)

# statuses as of TODAY
for p in policies:
    paid_sum = sum((row[1] for row in p["sched"] if row[3] is not None), D(0))
    if p["term_at"]:
        tdate = p["term_at"].date()
        total = (p["end"] - p["start"]).days + 1
        rem = max(0, (p["end"] - tdate).days)
        refund = r2(paid_sum * D(rem) / D(total) * D("0.75") - p["payouts"])
        p["status"], p["refund"] = "TERMINATED", max(refund, D("0.00"))
        for row in p["sched"]:
            if row[3] is None and row[2] > tdate: pass
        p["sched"] = [row for row in p["sched"] if row[3] is not None or row[2] <= tdate]
    elif p["payouts"] >= p["sum"]:
        p["status"] = "FULFILLED"
    elif p["sched"][0][3] is None:
        p["status"] = "WAITING_PAYMENT"
    elif p["end"] < TODAY:
        p["status"] = "EXPIRED"
    else:
        over = [row for row in p["sched"] if row[3] is None and row[2] < TODAY]
        if over and (TODAY - over[0][2]).days > 30:
            p["status"] = "TERMINATED"
            p["term_at"] = datetime.combine(over[0][2] + timedelta(days=31), datetime.min.time()) + timedelta(hours=3)
            p["refund"] = D("0.00")
        elif over:
            p["status"] = "SUSPENDED"
        else:
            p["status"] = "ACTIVE"

for p in policies:
    ins("policies", ["policy_number", "application_id", "client_id", "agent_id", "type_id", "insured_sum", "premium", "franchise",
                     "start_date", "end_date", "status", "created_at", "terminated_at", "refund_amount"],
        [p["num"], p["app"], p["client"]["id"], p["agent"], p["type"], p["sum"], p["prem"], p["fr"], p["start"], p["end"],
         p["status"], p["created"], p["term_at"], p["refund"]])
    for row in p["sched"]:
        ins("payments", ["policy_id", "installment_no", "amount", "due_date", "paid_date"], [p["id"], row[0], row[1], row[2], row[3]])
for c in claim_rows:
    ins("claims", ["policy_id", "incident_date", "report_date", "description", "damage_amount", "assessed_damage", "status",
                   "payout_amount", "agent_id", "decision_comment", "decided_at"],
        [c["p"]["id"], c["inc"], c["rep"], c["descr"], c["dmg"], c["assessed"], c["status"], c["payout"],
         None if c["status"] == "REPORTED" else c["p"]["agent"], c["comment"], c["dec"]])

# new (pending) and rejected applications
for k, (cli, ti, s, term, fr, n, descr, st) in enumerate([
        (2, 3, 25000, 12, 1, 4, "Kia Rio, 2022 г.в., госномер 7788 AE-7", "NEW"),
        (5, 1, 120000, 12, 0, 2, "Квартира, 3 комнаты, 82 м², монолитный дом 2020 г.", "NEW"),
        (10, 5, 30000, 1, 0, 1, "Горнолыжный отдых в Андорре", "NEW"),
        (13, 4, 20000, 12, 0, 12, "Страхование на время работы и в быту", "NEW"),
        (7, 3, 180000, 12, 0, 1, "Porsche Cayenne, 2012 г.в., без противоугонной системы", "REJECTED")]):
    created = datetime.combine(TODAY - timedelta(days=4 - min(k, 3)), datetime.min.time()) + timedelta(hours=10 + k)
    ins("applications", ["client_id", "type_id", "agent_id", "requested_sum", "term_months", "franchise_percent", "installments",
                         "object_description", "status", "decision_comment", "created_at", "decided_at"],
        [cli, ti, 2 if st == "REJECTED" else None, D(s), term, fr, n, descr, st,
         "Транспортное средство не оборудовано противоугонной системой" if st == "REJECTED" else None,
         created, created + timedelta(hours=5) if st == "REJECTED" else None])

E("COMMIT;")
for t in ("users", "clients", "agents", "insurance_types", "applications", "policies", "claims"):
    E(f"SELECT setval(pg_get_serial_sequence('{t}', 'id'), (SELECT max(id) FROM {t}));")
OUT.write_text("\n".join(lines) + "\n", encoding="utf8")
from collections import Counter
print("policies", len(policies), Counter(p["status"] for p in policies))
print("claims", len(claim_rows), Counter(c["status"] for c in claim_rows))
