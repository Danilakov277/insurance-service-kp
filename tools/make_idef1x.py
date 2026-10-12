"""IDEF1X information model (key-based / fully attributed) via Graphviz."""
import subprocess, sys
from pathlib import Path
OUT = Path("/home/claude/kp/diagrams")

ENT = {
 "roles":          (["id"], ["name"], False),
 "users":          (["id"], ["login", "password_hash", "salt", "role_id (FK)", "is_blocked", "created_at"], False),
 "clients":        (["id"], ["user_id (FK)", "last_name", "first_name", "patronymic", "birth_date", "passport_no", "phone", "address"], False),
 "agents":         (["id"], ["user_id (FK)", "last_name", "first_name", "patronymic", "position", "hire_date", "commission_rate"], False),
 "insurance_types":(["id"], ["name", "description", "base_rate", "min_sum", "max_sum", "max_term_months", "is_active"], False),
 "applications":   (["id"], ["client_id (FK)", "type_id (FK)", "agent_id (FK)", "requested_sum", "term_months", "franchise_percent", "installments", "object_description", "status", "decision_comment", "created_at", "decided_at"], False),
 "policies":       (["id"], ["policy_number", "application_id (FK)", "client_id (FK)", "agent_id (FK)", "type_id (FK)", "insured_sum", "premium", "franchise", "start_date", "end_date", "status", "created_at", "terminated_at", "refund_amount"], False),
 "payments":       (["policy_id (FK)", "installment_no"], ["amount", "due_date", "paid_date"], True),
 "claims":         (["id"], ["policy_id (FK)", "incident_date", "report_date", "description", "damage_amount", "status", "payout_amount", "agent_id (FK)", "decision_comment", "decided_at"], False),
}
# parent, child, identifying, cardinality label (child side), verb
REL = [
 ("roles", "users", False, "P", "определяет"),
 ("users", "clients", False, "Z", "является"),
 ("users", "agents", False, "Z", "является"),
 ("clients", "applications", False, "", "подаёт"),
 ("insurance_types", "applications", False, "", "выбран в"),
 ("agents", "applications", False, "", "рассматривает"),
 ("applications", "policies", False, "Z", "оформлена в"),
 ("clients", "policies", False, "", "заключает"),
 ("agents", "policies", False, "", "оформляет"),
 ("insurance_types", "policies", False, "", "определяет"),
 ("policies", "payments", True, "P", "содержит"),
 ("policies", "claims", False, "", "покрывает"),
 ("agents", "claims", False, "", "урегулирует"),
]

def node(name, pk, attrs, dep):
    style = ' style="rounded"' if dep else ''
    rows_pk = "".join(f'<TR><TD ALIGN="LEFT">{a}</TD></TR>' for a in pk)
    rows = "".join(f'<TR><TD ALIGN="LEFT">{a}</TD></TR>' for a in attrs)
    return (f'"{name}" [label=<<TABLE BORDER="0" CELLBORDER="0" CELLSPACING="0" CELLPADDING="1">'
            f'<TR><TD><B>{name}</B></TD></TR><TR><TD>'
            f'<TABLE PORT="b" BORDER="1.5" CELLBORDER="0" CELLSPACING="0" CELLPADDING="3"{style}>{rows_pk}'
            f'<HR/>{rows}</TABLE></TD></TR></TABLE>>];')

g = ['digraph G {', 'graph [rankdir=LR, splines=ortho, nodesep=0.6, ranksep=1.3, forcelabels=true, pad=0.3, fontname="Liberation Serif"];',
     'node [shape=plaintext, fontname="Liberation Serif", fontsize=13];',
     'edge [fontname="Liberation Serif", fontsize=12, arrowhead=dot, arrowtail=none, arrowsize=0.9, penwidth=1.3];']
for n, (pk, at, dep) in ENT.items():
    g.append(node(n, pk, at, dep))
for p, c, ident, card, verb in REL:
    st = "solid" if ident else "dashed"
    hl = f', headlabel="{card}", labeldistance=1.6, labelangle=35' if card else ''
    xl = "" if verb in ("оформлена в", "покрывает") else f'xlabel=" {verb} ", '
    g.append(f'"{p}":b -> "{c}":b [style={st}, {xl}dummy=1{hl}];'.replace(", dummy=1", "").replace("dummy=1", ""))
g.append('{rank=same; "roles";} {rank=same; "users"; "insurance_types";} {rank=same; "clients"; "agents";}')
g.append('}')
dot = "\n".join(g)
(OUT / "idef1x.dot").write_text(dot, encoding="utf8")
subprocess.run(["dot", "-Tpng", "-Gdpi=170", str(OUT / "idef1x.dot"), "-o", str(OUT / "idef1x.png")], check=True)
subprocess.run(["dot", "-Tsvg", str(OUT / "idef1x.dot"), "-o", str(OUT / "idef1x.svg")], check=True)
print("ok")
