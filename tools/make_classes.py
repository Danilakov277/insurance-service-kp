"""Generate PlantUML class diagrams from the actual Java sources (so diagrams match the code).

usage: python3 make_classes.py  -> writes uml/classes_model.puml, uml/classes_server.puml, uml/classes_poster.puml
"""
import re
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
SRC = {
    "common": ROOT / "common/src/main/java/by/bsuir/insurance/common",
    "server": ROOT / "server/src/main/java/by/bsuir/insurance/server",
    "client": ROOT / "client/src/main/java/by/bsuir/insurance/client",
}

DECL = re.compile(r"^(public\s+)?((?:abstract|final)\s+)*(class|interface|enum)\s+(\w+)(?:<[^>]*>)?"
                  r"(?:\s+extends\s+([\w.<>, ]+?))?(?:\s+implements\s+([\w.<>, ]+?))?\s*\{", re.M)
FIELD = re.compile(r"^    (private|protected|public)\s+((?:static\s+|final\s+|volatile\s+)*)([\w.<>\[\], ?]+?)\s+(\w+)\s*(=[^;]*)?;", re.M)
METHOD = re.compile(r"^    (public|protected)\s+((?:static\s+|abstract\s+|final\s+|synchronized\s+)*)(<[^>]+>\s+)?"
                    r"([\w.<>\[\], ?]+?\s+)?(\w+)\s*\(([^)]*)\)\s*(?:throws [\w, .]+)?\s*[{;]", re.M)
IFACE_METHOD = re.compile(r"^    (?:default\s+|static\s+)?([\w.<>\[\], ?]+?)\s+(\w+)\s*\(([^)]*)\)\s*(?:throws [\w, .]+)?\s*;", re.M)
ENUM_CONST = re.compile(r"^    ([A-Z_]+)\s*[(,;]", re.M)


def short(t):
    t = t.strip()
    t = re.sub(r"\b(?:java\.\w+\.)+", "", t)
    return t.replace("final ", "")


COMPACT = False


def params(p):
    out = []
    if COMPACT:
        return ", ".join(part.replace("final ", "").strip().rsplit(" ", 1)[-1]
                         for part in re.split(r",(?![^<]*>)", p) if part.strip())
    for part in [x.strip() for x in re.split(r",(?![^<]*>)", p) if x.strip()]:
        bits = part.replace("final ", "").rsplit(" ", 1)
        out.append(f"{bits[1]} : {short(bits[0])}" if len(bits) == 2 else part)
    return ", ".join(out)


def parse(path):
    s = path.read_text(encoding="utf8")
    m = DECL.search(s)
    if not m:
        return None
    mods = (m.group(2) or "")
    kind = m.group(3)
    c = {"name": m.group(4), "kind": "abstract" if "abstract" in mods and kind == "class" else kind,
         "extends": short(m.group(5)) if m.group(5) else None,
         "implements": [short(x) for x in (m.group(6) or "").split(",") if x.strip()],
         "fields": [], "methods": [], "consts": [], "pkg": path.parent.name}
    body = s[m.end():]
    # cut nested types
    body = re.split(r"\n    (?:public |private |protected )?(?:static )?(?:final )?(?:class|interface|enum) ", body)[0]
    if kind == "enum":
        c["consts"] = ENUM_CONST.findall(body.split(";")[0] + ";")
    for f in FIELD.finditer(body):
        vis, mods_, typ, name = f.group(1), f.group(2), f.group(3), f.group(4)
        if name == "serialVersionUID" or ("static" in mods_ and "final" in mods_ and name.isupper() and vis == "private"):
            continue
        sym = {"private": "-", "protected": "#", "public": "+"}[vis]
        st = "{static} " if "static" in mods_ else ""
        c["fields"].append(f"{sym}{st}{name} : {short(typ)}")
    if kind == "interface":
        for mm in IFACE_METHOD.finditer(body):
            c["methods"].append(f"+{{abstract}} {mm.group(2)}({params(mm.group(3))}) : {short(mm.group(1))}")
    for mm in METHOD.finditer(body):
        vis, mods_, ret, name, ps = mm.group(1), mm.group(2), mm.group(4), mm.group(5), mm.group(6)
        sym = "+" if vis == "public" else "#"
        tags = ("{static} " if "static" in mods_ else "") + ("{abstract} " if "abstract" in mods_ else "")
        if ret is None or ret.strip() in ("new", "return"):
            if name == c["name"]:
                continue  # constructor
            continue
        r = short(ret)
        c["methods"].append(f"{sym}{tags}{name}({params(ps)}) : {r}")
    return c


def load(module, sub):
    out = {}
    for p in sorted((SRC[module] / sub).glob("*.java")):
        c = parse(p)
        if c:
            out[c["name"]] = c
    return out


def render(c, show_fields=True, show_methods=True, only=None, max_methods=99):
    kw = {"interface": "interface", "enum": "enum", "abstract": "abstract class", "class": "class"}[c["kind"]]
    lines = [f'{kw} {c["name"]} {{']
    if c["kind"] == "enum":
        lines += [f"  {x}" for x in c["consts"]]
    if show_fields:
        lines += [f"  {f}" for f in c["fields"] if not only or f.split(" :")[0].lstrip("-#+").replace("{static} ", "") in only]
    if show_methods:
        ms = [m for m in c["methods"] if not only or m.split("(")[0].split()[-1].lstrip("+#") in only]
        lines += [f"  {m}" for m in ms[:max_methods]]
    lines.append("}")
    return "\n".join(lines)


HEAD = """@startuml
!include _style.iuml
skinparam classAttributeIconSize 0
skinparam defaultFontSize {fs}
skinparam nodesep {ns}
skinparam ranksep {rs}
hide circle
hide empty members
set separator none
"""


def inheritance(classes):
    rel = []
    names = set(classes)
    for c in classes.values():
        if c["extends"] and c["extends"].split("<")[0] in names:
            rel.append(f'{c["extends"].split("<")[0]} <|-- {c["name"]}')
        for i in c["implements"]:
            i = i.split("<")[0]
            if i in names:
                rel.append(f"{i} <|.. {c['name']}")
    return rel


def model_diagram():
    m = load("common", "model")
    keep = ["Entity", "Person", "Client", "Agent", "User", "Role", "InsuranceType", "InsuranceCategory",
            "Application", "ApplicationStatus", "Policy", "PolicyStatus", "Payment", "Claim", "ClaimStatus"]
    cls = {k: m[k] for k in keep if k in m}
    out = [HEAD.format(fs=12, ns=14, rs=30), "left to right direction", 'package "by.bsuir.insurance.common.model" {']
    for k in keep:
        if k not in cls:
            continue
        c = cls[k]
        # fields only for entities; getters/setters omitted, domain methods kept
        dom = [x for x in c["methods"] if not re.match(r"[+#](?:\{\w+\} )*(get|set|is)[A-Z]\w*\(", x)
               or re.match(r"[+#](?:\{\w+\} )*(isActiveOn|isOverdue|isPaid|getAge|getFullName|getShortName|getDisplayName|isClosed|allowsClaims|getTitle)\(", x)]
        c2 = dict(c, methods=dom)
        out.append(render(c2))
    out.append("}")
    out += inheritance(cls)
    out += ['Policy "1" *-- "1..*" Payment : график взносов',
            'Policy "1" o-- "0..*" Claim : страховые случаи',
            'Client "1" -- "0..*" Application',
            'Application "1" -- "0..1" Policy',
            'InsuranceType "1" -- "0..*" Policy',
            'InsuranceType --> InsuranceCategory',
            'User --> Role', 'Policy --> PolicyStatus', 'Application --> ApplicationStatus', 'Claim --> ClaimStatus']
    out.append("@enduml")
    return "\n".join(out)


def _block(out, pkg, classes, keep, methods_only=None, fields=False):
    out.append(f'package "{pkg}" {{')
    for k in keep:
        if k in classes and classes[k]:
            c = classes[k]
            ms = c["methods"]
            if methods_only is not None:
                ms = [x for x in ms if re.sub(r"\{\w+\} ", "", x)[1:].split("(")[0] in methods_only.get(k, [])]
            out.append(render(dict(c, methods=ms), show_fields=fields))
    out.append("}")


def server_core_diagram():
    net = load("server", "net")
    db = load("server", "db")
    dao = load("server", "dao")
    svc = load("server", "service")
    root = {p.stem: parse(p) for p in (SRC["server"]).glob("*.java")}
    out = [HEAD.format(fs=12, ns=18, rs=40), "left to right direction"]
    _block(out, "server", root, ["ServerApp", "ServerConfig"],
           {"ServerApp": ["main"], "ServerConfig": ["getInstance", "load", "getPort", "getThreads"]})
    _block(out, "server.net", net, ["ClientHandler", "Session", "CommandDispatcher", "CommandHandler"],
           {"ClientHandler": ["run"], "CommandDispatcher": ["dispatch"], "CommandHandler": ["handle"],
            "Session": ["isAuthenticated", "personId"]})
    _block(out, "server.service", svc, ["AuthService", "ApplicationService", "PolicyService", "ClaimService",
                                         "AdminService", "ReportService"],
           {"AuthService": ["login", "registerClient"],
            "ApplicationService": ["quote", "submit", "approve", "reject"],
            "PolicyService": ["payInstallment", "renew", "previewRefund", "terminate", "controlList", "refreshStatuses"],
            "ClaimService": ["report", "preview", "settle"],
            "AdminService": ["saveType", "createAgent", "setBlocked"], "ReportService": ["sales", "lossRatio"]})
    _block(out, "server.db", db, ["ConnectionManager"],
           {"ConnectionManager": ["getInstance", "acquire", "release", "withConnection", "inTransaction"]})
    _block(out, "server.dao", dao, ["AbstractDao", "UserDao", "ClientDao", "AgentDao", "InsuranceTypeDao",
                                     "ApplicationDao", "PolicyDao", "PaymentDao", "ClaimDao", "ReportDao"],
           {"AbstractDao": ["queryList", "queryOne", "update", "insert", "map"]})
    out += inheritance({**net, **dao})
    out += ["ServerApp ..> ServerConfig", "ServerApp ..> ClientHandler : создаёт в пуле потоков",
            "ClientHandler --> CommandDispatcher", "ClientHandler *-- Session",
            'CommandDispatcher o-- "*" CommandHandler',
            "CommandDispatcher ..> AuthService", "CommandDispatcher ..> ApplicationService",
            "CommandDispatcher ..> PolicyService", "CommandDispatcher ..> ClaimService",
            "CommandDispatcher ..> AdminService", "CommandDispatcher ..> ReportService",
            "ApplicationService ..> ConnectionManager", "PolicyService ..> ConnectionManager",
            "ClaimService ..> ConnectionManager",
            "ApplicationService ..> ApplicationDao", "PolicyService ..> PolicyDao", "ClaimService ..> ClaimDao",
            "ReportService ..> ReportDao", "AuthService ..> UserDao", "AdminService ..> InsuranceTypeDao"]
    out.append("@enduml")
    return "\n".join(out)


def server_logic_diagram():
    logic = load("server", "logic")
    out = [HEAD.format(fs=12, ns=25, rs=40)]
    _block(out, "server.logic", logic, ["PremiumCalculator", "PremiumStrategyFactory", "PremiumStrategy",
                                         "AbstractPremiumStrategy", "PropertyPremiumStrategy", "CascoPremiumStrategy",
                                         "AccidentPremiumStrategy", "TravelPremiumStrategy", "InstallmentScheduler",
                                         "PayoutCalculator", "RefundCalculator", "PolicyStatusRules"],
           {"PremiumCalculator": ["calculate", "validate", "ageFactor", "termFactor", "bonusMalusFactor", "franchiseFactor"],
            "PremiumStrategy": ["typeFactor", "factorName"],
            "AbstractPremiumStrategy": ["typeFactor", "extraFactor", "riskKeywords", "riskSurcharge"],
            "PremiumStrategyFactory": ["forType"], "PayoutCalculator": ["calculate"], "RefundCalculator": ["calculate"],
            "InstallmentScheduler": ["build", "validate"], "PolicyStatusRules": ["evaluate"],
            "PropertyPremiumStrategy": ["extraFactor", "riskKeywords", "riskSurcharge", "factorName"],
            "CascoPremiumStrategy": ["extraFactor", "factorName"],
            "AccidentPremiumStrategy": ["riskKeywords", "riskSurcharge", "factorName"],
            "TravelPremiumStrategy": ["riskKeywords", "riskSurcharge", "factorName"]})
    out += inheritance(logic)
    out += ["PremiumCalculator ..> PremiumStrategyFactory", "PremiumStrategyFactory ..> PremiumStrategy : создаёт",
            "PremiumCalculator ..> InstallmentScheduler"]
    out.append("@enduml")
    return "\n".join(out)


if __name__ == "__main__":
    global_ns = globals()
    uml = ROOT / "uml"
    (uml / "classes_model.puml").write_text(model_diagram(), encoding="utf8")
    global_ns["COMPACT"] = True
    (uml / "classes_server.puml").write_text(server_core_diagram(), encoding="utf8")
    (uml / "classes_logic.puml").write_text(server_logic_diagram(), encoding="utf8")
    print("ok")
