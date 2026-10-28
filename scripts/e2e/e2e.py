"""Run: python -I scripts/e2e/e2e.py (API on :8082, Mailpit on :8025, dev profile).
The run creates data prefixed "E2E"; remove it with scripts/e2e/cleanup.sql.

E2E smoke/contract test for workshop-api covering every route and all roles."""
import json, uuid, urllib.request, urllib.error, datetime as dt, re, time, sys, io

BASE = "http://localhost:8082/api/v1"
MAILPIT = "http://localhost:8025/api/v1"
PWD = "Workshop@2026!"
RESULTS = []
COVERED = set()


def req(method, path, token=None, body=None, headers=None, raw=False, expect=None, label=None, tmpl=None):
    url = path if path.startswith("http") else BASE + path
    h = {"Accept": "application/json"}
    data = None
    if body is not None and not isinstance(body, bytes):
        data = json.dumps(body).encode()
        h["Content-Type"] = "application/json"
    elif isinstance(body, bytes):
        data = body
    if token:
        h["Authorization"] = "Bearer " + token
    h.update(headers or {})
    r = urllib.request.Request(url, data=data, method=method, headers=h)
    t0 = time.time()
    try:
        with urllib.request.urlopen(r, timeout=30) as resp:
            status, content, rh = resp.status, resp.read(), dict(resp.headers)
    except urllib.error.HTTPError as e:
        status, content, rh = e.code, e.read(), dict(e.headers)
    ms = int((time.time() - t0) * 1000)
    if tmpl:
        COVERED.add((method, tmpl))
    parsed = content
    if not raw:
        try:
            parsed = json.loads(content) if content else None
        except Exception:
            parsed = content[:200]
    if expect is not None:
        ok = status in (expect if isinstance(expect, (list, tuple, set)) else [expect])
        RESULTS.append((ok, label or f"{method} {path}", status, expect, ms,
                        None if ok else (parsed if not raw else content[:200])))
        mark = "PASS" if ok else "FAIL"
        print(f"[{mark}] {status} ({ms}ms) {label or method + ' ' + path}" + ("" if ok else f"  expected={expect} body={str(parsed)[:300]}"))
    return status, parsed, rh


def login(user, pwd=PWD):
    s, b, _ = req("POST", "/auth/login", body={"login": user, "password": pwd}, expect=200,
                  label=f"login {user}", tmpl="/auth/login")
    return b


def key():
    return {"Idempotency-Key": str(uuid.uuid4())}


def check(cond, label, detail=""):
    RESULTS.append((bool(cond), label, "-", "-", 0, None if cond else detail))
    print(f"[{'PASS' if cond else 'FAIL'}] {label}" + ("" if cond else f"  -> {detail}"))


today = dt.date.today()
SUF = uuid.uuid4().hex[:6]

print("\n=== AUTH ===")
req("POST", "/auth/login", body={"login": "demo.ana", "password": "errada"}, expect=401, label="login inválido", tmpl="/auth/login")
req("POST", "/auth/login", body={}, expect=400, label="login payload vazio", tmpl="/auth/login")
sess = {u: login(u) for u in ["demo.admin", "demo.carla", "demo.ana", "demo.bruno"]}
for u, b in sess.items():
    check(b and b.get("accessToken") and b.get("refreshToken"), f"tokens emitidos {u}", b)
ADMIN, ARWEG, ANA, BRUNO = (sess[u]["accessToken"] for u in ["demo.admin", "demo.carla", "demo.ana", "demo.bruno"])
req("GET", "/users/me", expect=401, label="sem token -> 401", tmpl="/users/me")
req("GET", "/users/me", token="lixo.jwt.token", expect=401, label="token inválido -> 401", tmpl="/users/me")
# refresh rotation
tmp = login("demo.bruno")
s, rf, _ = req("POST", "/auth/refresh", body={"refreshToken": tmp["refreshToken"]}, expect=200, label="refresh", tmpl="/auth/refresh")
req("POST", "/auth/refresh", body={"refreshToken": tmp["refreshToken"]}, expect=401, label="refresh reutilizado rejeitado", tmpl="/auth/refresh")
req("POST", "/auth/logout", token=rf["accessToken"], body={"refreshToken": rf["refreshToken"]}, expect=[200, 204], label="logout", tmpl="/auth/logout")
req("POST", "/auth/refresh", body={"refreshToken": rf["refreshToken"]}, expect=401, label="refresh após logout rejeitado", tmpl="/auth/refresh")
req("POST", "/auth/forgot-password", body={"email": "naoexiste@x.com"}, expect=[200, 202, 204], label="forgot-password email desconhecido (sem enumeração)", tmpl="/auth/forgot-password")
req("POST", "/auth/reset-password", body={"token": "invalido", "newPassword": "Nova@Senha2026!"}, expect=[400, 401, 404], label="reset-password token inválido", tmpl="/auth/reset-password")

print("\n=== PERFIL / PREFERÊNCIAS ===")
me = {}
for name, t in [("admin", ADMIN), ("arweg", ARWEG), ("ana", ANA), ("bruno", BRUNO)]:
    _, me[name], _ = req("GET", "/users/me", token=t, expect=200, label=f"GET /users/me {name}", tmpl="/users/me")
import base64
def claims(t):
    p = t.split(".")[1]; p += "=" * (-len(p) % 4)
    return json.loads(base64.urlsafe_b64decode(p))
roles = {n: claims(t) for n, t in [("admin", ADMIN), ("arweg", ARWEG), ("ana", ANA)]}
print("JWT claims exemplo:", roles["ana"])
check("role" not in me["ana"], "OBS: /users/me não expõe role (cliente depende do JWT)", "")
_, themes, _ = req("GET", "/themes", token=ANA, expect=200, label="GET /themes", tmpl="/themes")
_, cats, _ = req("GET", "/categories", token=ANA, expect=200, label="GET /categories", tmpl="/categories")
theme_list = themes if isinstance(themes, list) else themes.get("content", [])
cat_list = cats if isinstance(cats, list) else cats.get("content", [])
THEME, CAT = theme_list[0]["id"], cat_list[0]["id"]
req("PATCH", "/users/me", token=ANA, body={"name": me["ana"]["name"], "phone": "47999990000"}, expect=200, label="PATCH /users/me", tmpl="/users/me")
req("PATCH", "/users/me", token=ANA, body={"name": ""}, expect=400, label="PATCH /users/me nome vazio -> 400", tmpl="/users/me")
req("PUT", "/users/me/themes", token=ANA, body={"themeIds": [t["id"] for t in theme_list[:2]]}, expect=[200, 204], label="PUT /users/me/themes", tmpl="/users/me/themes")
req("PUT", "/users/me/themes", token=ANA, body={"themeIds": [str(uuid.uuid4())]}, expect=[400, 404, 422], label="PUT themes inexistente", tmpl="/users/me/themes")

print("\n=== ADMIN: TAXONOMIA / USUÁRIOS ===")
_, th, _ = req("POST", "/admin/themes", token=ADMIN, body={"name": f"Tema E2E {SUF}", "description": "teste"}, expect=201, label="admin cria tema", tmpl="/admin/themes")
req("PATCH", f"/admin/themes/{th['id']}", token=ADMIN, body={"active": False}, expect=200, label="admin desativa tema", tmpl="/admin/themes/{id}")
_, ct, _ = req("POST", "/admin/categories", token=ADMIN, body={"name": f"Cat E2E {SUF}"}, expect=201, label="admin cria categoria", tmpl="/admin/categories")
req("PATCH", f"/admin/categories/{ct['id']}", token=ADMIN, body={"description": "upd"}, expect=200, label="admin atualiza categoria", tmpl="/admin/categories/{id}")
req("POST", "/admin/themes", token=ADMIN, body={"name": f"Tema E2E {SUF}"}, expect=409, label="tema duplicado -> 409", tmpl="/admin/themes")
for name, t in [("arweg", ARWEG), ("participant", ANA)]:
    req("POST", "/admin/themes", token=t, body={"name": "x"}, expect=403, label=f"{name} cria tema -> 403", tmpl="/admin/themes")
    req("POST", "/admin/users", token=t, body={"name": "x", "username": "x", "email": "x@x.com", "role": "ADMIN"}, expect=403, label=f"{name} cria usuário -> 403", tmpl="/admin/users")
    req("GET", "/admin/audit", token=t, expect=403, label=f"{name} audit -> 403", tmpl="/admin/audit")
    req("GET", "/admin/metrics/workshops", token=t, expect=403, label=f"{name} metrics -> 403", tmpl="/admin/metrics/workshops")

# Provision a second ARWEG to exercise first-access + "unrelated ARWEG" isolation
uname = f"e2e.arweg.{SUF}"
_, nu, _ = req("POST", "/admin/users", token=ADMIN, body={"name": "ARWEG E2E", "username": uname, "email": f"{uname}@example.com", "role": "ARWEG"}, expect=201, label="admin provisiona ARWEG", tmpl="/admin/users")
req("POST", "/admin/users", token=ADMIN, body={"name": "dup", "username": uname, "email": f"{uname}@example.com", "role": "ARWEG"}, expect=409, label="usuário duplicado -> 409", tmpl="/admin/users")
time.sleep(1.5)
temp_pwd = None
try:
    _, msgs, _ = req("GET", f"{MAILPIT}/search?query=to:{uname}@example.com")
    mid = msgs["messages"][0]["ID"]
    _, msg, _ = req("GET", f"{MAILPIT}/message/{mid}")
    text = msg.get("Text", "")
    m = re.search(r"(?:senha|password)[^:\n]*:\s*(\S+)", text, re.I)
    temp_pwd = m.group(1) if m else None
except Exception as e:
    print("mailpit:", e)
check(temp_pwd, "e-mail de primeiro acesso entregue com senha temporária", "não encontrado no Mailpit")
OTHER_ARWEG = None
if temp_pwd:
    first = login(uname, temp_pwd)
    check(first.get("mustChangePassword") is True, "mustChangePassword=true no primeiro acesso", first)
    req("GET", "/workshops?size=1", token=first["accessToken"], expect=403, label="senha temporária bloqueia outras rotas", tmpl="/workshops")
    sw, _, _ = req("POST", "/auth/change-password", token=first["accessToken"], body={"currentPassword": temp_pwd, "newPassword": "fraca"}, expect=400, label="change-password senha fraca -> 400", tmpl="/auth/change-password")
    if sw == 204:  # weak password accepted: recover so the remaining flow can continue
        first = login(uname, "fraca"); temp_pwd = "fraca"
    req("POST", "/auth/change-password", token=first["accessToken"], body={"currentPassword": temp_pwd, "newPassword": PWD}, expect=[200, 204], label="change-password primeiro acesso", tmpl="/auth/change-password")
    req("GET", "/users/me", token=first["accessToken"], expect=401, label="token antigo revogado após troca de senha", tmpl="/users/me")
    OTHER_ARWEG = login(uname)["accessToken"]

print("\n=== WORKSHOPS (ARWEG) ===")


def wpayload(title, pay="FREE", price=0, cap=10, start_offset=10, champ=False):
    sd = today + dt.timedelta(days=start_offset)
    return {"title": title, "description": "Workshop criado pelo teste E2E", "themeId": THEME, "categoryId": CAT,
            "startDate": sd.isoformat(), "endDate": sd.isoformat(), "startTime": "09:00", "endTime": "12:00",
            "location": "Jaraguá do Sul", "modality": "IN_PERSON", "price": price,
            "registrationStart": dt.datetime.combine(today - dt.timedelta(days=1), dt.time()).isoformat() + "Z",
            "registrationEnd": dt.datetime.combine(sd, dt.time(8, 0)).isoformat() + "Z",
            "maximumParticipants": cap, "paymentMethod": pay, "championship": champ}


s, paid_w, _ = req("POST", "/workshops", token=ARWEG, body=wpayload(f"E2E Pago {SUF}", "PIX", 150.0, cap=1), expect=201, label="ARWEG cria workshop pago (PIX, 1 vaga)", tmpl="/workshops")
if s != 201:
    # Try alternate date-time format (LocalDateTime without Z)
    p = wpayload(f"E2E Pago {SUF}", "PIX", 150.0, cap=1)
    p["registrationStart"] = p["registrationStart"].rstrip("Z"); p["registrationEnd"] = p["registrationEnd"].rstrip("Z")
    s, paid_w, _ = req("POST", "/workshops", token=ARWEG, body=p, expect=201, label="ARWEG cria workshop pago (formato sem Z)", tmpl="/workshops")
NOZ = s == 201 and "Z" not in json.dumps(wpayload("x")) and False


def mk(token, *a, **k):
    p = wpayload(*a, **k)
    st, b, _ = req("POST", "/workshops", token=token, body=p, expect=201, label=f"cria workshop {a[0]}", tmpl="/workshops")
    if st == 400:
        p["registrationStart"] = p["registrationStart"].rstrip("Z"); p["registrationEnd"] = p["registrationEnd"].rstrip("Z")
        st, b, _ = req("POST", "/workshops", token=token, body=p, expect=201, label=f"cria workshop (retry fmt) {a[0]}", tmpl="/workshops")
    return b


free_w = mk(ARWEG, f"E2E Gratuito {SUF}", "FREE", 0, 5)
card_w = mk(ARWEG, f"E2E Cartão próximo {SUF}", "CREDIT_CARD", 99.9, 5, start_offset=1)
champ_w = mk(ARWEG, f"E2E Campeonato {SUF}", "CREDIT_CARD", 50, 5, start_offset=1, champ=True)
req("POST", "/workshops", token=ANA, body=wpayload("x"), expect=403, label="participante cria workshop -> 403", tmpl="/workshops")
bad = wpayload("datas invertidas"); bad["endDate"] = (today - dt.timedelta(days=5)).isoformat()
req("POST", "/workshops", token=ARWEG, body=bad, expect=400, label="workshop com fim antes do início -> 400", tmpl="/workshops")
neg = wpayload("preço negativo", "PIX", -10)
req("POST", "/workshops", token=ARWEG, body=neg, expect=400, label="workshop pago com preço negativo -> 400", tmpl="/workshops")
fp = wpayload("free com preço", "FREE", 50)
req("POST", "/workshops", token=ARWEG, body=fp, expect=400, label="workshop FREE com preço>0 -> 400", tmpl="/workshops")
pz = wpayload("pix preço zero", "PIX", 0)
req("POST", "/workshops", token=ARWEG, body=pz, expect=400, label="workshop PIX com preço 0 -> 400", tmpl="/workshops")

PW, FW, CW, CHW = paid_w["id"], free_w["id"], card_w["id"], champ_w["id"]
upd = wpayload(f"E2E Pago {SUF} (editado)", "PIX", 150.0, cap=1)
req("PUT", f"/workshops/{PW}", token=ARWEG, body=upd, expect=200, label="ARWEG edita DRAFT", tmpl="/workshops/{id}")
if OTHER_ARWEG:
    req("PUT", f"/workshops/{PW}", token=OTHER_ARWEG, body=upd, expect=[403, 404], label="ARWEG não-dono edita -> 403/404", tmpl="/workshops/{id}")
    req("PATCH", f"/workshops/{PW}/publish", token=OTHER_ARWEG, expect=[403, 404], label="ARWEG não-dono publica -> 403/404", tmpl="/workshops/{id}/publish")
req("GET", f"/workshops/{PW}", token=ANA, expect=404, label="participante não vê DRAFT", tmpl="/workshops/{id}")
s, dup, _ = req("POST", f"/workshops/{PW}/duplicate", token=ARWEG, expect=201, label="duplicar workshop", tmpl="/workshops/{id}/duplicate")
sch = (dt.datetime.utcnow() + dt.timedelta(days=3)).replace(microsecond=0).isoformat() + "Z"
req("PATCH", f"/workshops/{dup['id']}/schedule", token=ARWEG, body={"scheduledPublishAt": sch}, expect=200, label="agendar publicação", tmpl="/workshops/{id}/schedule")
for wid in [PW, FW, CW, CHW]:
    req("PATCH", f"/workshops/{wid}/publish", token=ARWEG, expect=200, label=f"publicar {wid[:8]}", tmpl="/workshops/{id}/publish")
req("PUT", f"/workshops/{PW}", token=ARWEG, body=upd, expect=[400, 409, 422], label="editar PUBLISHED bloqueado", tmpl="/workshops/{id}")
req("PATCH", f"/workshops/{PW}/archive", token=ARWEG, expect=[400, 409, 422], label="PUBLISHED -> ARCHIVED inválido", tmpl="/workshops/{id}/archive")
s, det, h = req("GET", f"/workshops/{PW}", token=ANA, expect=200, label="participante vê detalhe publicado", tmpl="/workshops/{id}")
etag = h.get("ETag") or h.get("Etag")
check(etag, "ETag no detalhe", h)
if etag:
    req("GET", f"/workshops/{PW}", token=ANA, headers={"If-None-Match": etag}, expect=304, label="If-None-Match -> 304", tmpl="/workshops/{id}")
_, wl, _ = req("GET", "/workshops?size=50&sort=startDate,asc", token=ANA, expect=200, label="lista workshops", tmpl="/workshops")
req("GET", f"/workshops?themeId={THEME}&size=5", token=ANA, expect=200, label="filtra por tema", tmpl="/workshops")
req("GET", "/workshops?status=DRAFT", token=ANA, expect=[200, 400, 403], label="participante filtra DRAFT", tmpl="/workshops")
_, wdraft, _ = req("GET", "/workshops?status=DRAFT&size=50", token=ANA)
if isinstance(wdraft, dict):
    check(not any(w.get("status") == "DRAFT" for w in wdraft.get("content", [])), "participante não recebe DRAFT na listagem", wdraft)
upd_after = (dt.datetime.utcnow() - dt.timedelta(minutes=10)).replace(microsecond=0).isoformat() + "Z"
req("GET", f"/workshops?updatedAfter={upd_after}", token=ANA, expect=200, label="workshops updatedAfter", tmpl="/workshops")

print("\n=== MÍDIA ===")
PNG = bytes.fromhex("89504e470d0a1a0a0000000d4948445200000001000000010806000000" "1f15c4890000000d49444154789c6360000002000154a24f5d0000000049454e44ae426082")


def multipart(field, filename, content, ctype):
    b = "----e2e" + uuid.uuid4().hex
    body = (f"--{b}\r\nContent-Disposition: form-data; name=\"{field}\"; filename=\"{filename}\"\r\nContent-Type: {ctype}\r\n\r\n").encode() + content + f"\r\n--{b}--\r\n".encode()
    return body, {"Content-Type": f"multipart/form-data; boundary={b}"}


body, hh = multipart("file", "capa.png", PNG, "image/png")
req("POST", f"/workshops/{FW}/image", token=ARWEG, body=body, headers=hh, expect=[200, 201], label="upload capa", tmpl="/workshops/{workshopId}/image")
req("GET", f"/workshops/{FW}/image/content", token=ANA, raw=True, expect=200, label="baixar capa", tmpl="/workshops/{workshopId}/image/content")
body, hh = multipart("file", "malware.exe", b"MZ....", "application/octet-stream")
req("POST", f"/workshops/{FW}/attachments", token=ARWEG, body=body, headers=hh, expect=[400, 415, 422], label="anexo .exe rejeitado", tmpl="/workshops/{workshopId}/attachments")
body, hh = multipart("file", "apostila.png", PNG, "image/png")
s, att, _ = req("POST", f"/workshops/{FW}/attachments", token=ARWEG, body=body, headers=hh, expect=[200, 201], label="upload anexo PNG", tmpl="/workshops/{workshopId}/attachments")
req("GET", f"/workshops/{FW}/attachments", token=ANA, expect=200, label="listar anexos", tmpl="/workshops/{workshopId}/attachments")
if isinstance(att, dict) and att.get("id"):
    req("GET", f"/workshops/{FW}/attachments/{att['id']}/content", token=ANA, raw=True, expect=200, label="baixar anexo", tmpl="/workshops/{workshopId}/attachments/{attachmentId}/content")
    body, hh = multipart("file", "x.png", PNG, "image/png")
    req("POST", f"/workshops/{FW}/attachments", token=ANA, body=body, headers=hh, expect=[403, 404], label="participante sobe anexo -> 403", tmpl="/workshops/{workshopId}/attachments")
    req("DELETE", f"/workshops/{FW}/attachments/{att['id']}", token=ARWEG, expect=204, label="remover anexo", tmpl="/workshops/{workshopId}/attachments/{attachmentId}")
req("DELETE", f"/workshops/{FW}/image", token=ARWEG, expect=204, label="remover capa", tmpl="/workshops/{workshopId}/image")

print("\n=== INSCRIÇÃO + PAGAMENTO (fluxo crítico) ===")
k_ana = key()
s, reg_ana, _ = req("POST", f"/workshops/{PW}/registrations", token=ANA, headers=k_ana, expect=201, label="Ana inscreve-se no pago", tmpl="/workshops/{workshopId}/registrations")
check(reg_ana["status"] == "PENDING" and reg_ana["paymentStatus"] == "PENDING", "inscrição paga nasce PENDING/PENDING", reg_ana)
s, again, _ = req("POST", f"/workshops/{PW}/registrations", token=ANA, headers=k_ana, expect=[200, 201], label="replay idempotente da inscrição", tmpl="/workshops/{workshopId}/registrations")
check(again and again.get("id") == reg_ana["id"], "replay devolve mesma inscrição", again)
req("POST", f"/workshops/{PW}/registrations", token=ANA, headers=key(), expect=409, label="segunda inscrição (nova chave) -> 409", tmpl="/workshops/{workshopId}/registrations")
req("POST", f"/workshops/{PW}/registrations", token=ANA, expect=400, label="inscrição sem Idempotency-Key -> 400", tmpl="/workshops/{workshopId}/registrations")
req("POST", f"/workshops/{FW}/registrations", token=ANA, headers=k_ana, expect=409, label="reuso da chave em outro workshop -> 409", tmpl="/workshops/{workshopId}/registrations")
s, reg_bruno, _ = req("POST", f"/workshops/{PW}/registrations", token=BRUNO, headers=key(), expect=201, label="Bruno -> lista de espera", tmpl="/workshops/{workshopId}/registrations")
check(reg_bruno["status"] == "WAITING_LIST" and reg_bruno.get("waitingListPosition") == 1, "Bruno WAITING_LIST pos 1", reg_bruno)
req("GET", f"/workshops/{PW}/registrations/me", token=ANA, expect=200, label="minha inscrição", tmpl="/workshops/{workshopId}/registrations/me")
req("GET", f"/workshops/{PW}/registrations", token=ARWEG, expect=200, label="ARWEG lista inscrições", tmpl="/workshops/{workshopId}/registrations")
req("GET", f"/workshops/{PW}/registrations", token=ANA, expect=[403, 404], label="participante lista inscrições -> 403", tmpl="/workshops/{workshopId}/registrations")
if OTHER_ARWEG:
    req("GET", f"/workshops/{PW}/registrations", token=OTHER_ARWEG, expect=[403, 404], label="ARWEG não-dono lista inscrições -> 403/404", tmpl="/workshops/{workshopId}/registrations")

# Group access before payment
_, grp_list, _ = req("GET", "/groups?size=100", token=ARWEG, expect=200, label="ARWEG lista grupos", tmpl="/groups")
gcont = grp_list.get("content", []) if isinstance(grp_list, dict) else grp_list
PG = next((g["id"] for g in gcont if g.get("workshopId") == PW), None)
check(PG, "grupo criado para workshop pago", [g.get("workshopId") for g in gcont][:5])
if PG:
    req("GET", f"/groups/{PG}", token=ANA, expect=[403, 404], label="sem pagamento: sem acesso ao grupo", tmpl="/groups/{groupId}")

kp = key()
req("POST", f"/registrations/{reg_ana['id']}/payments", token=BRUNO, headers=key(), expect=[403, 404], label="pagar inscrição alheia -> 403/404", tmpl="/registrations/{registrationId}/payments")
req("POST", f"/registrations/{reg_ana['id']}/payments", token=ANA, expect=400, label="pagamento sem Idempotency-Key -> 400", tmpl="/registrations/{registrationId}/payments")
s, pay, _ = req("POST", f"/registrations/{reg_ana['id']}/payments", token=ANA, headers=kp, expect=201, label="Ana cria pagamento PIX", tmpl="/registrations/{registrationId}/payments")
check(pay["status"] == "PENDING" and float(pay["amount"]) == 150.0 and pay.get("method") in ("PIX", None), "pagamento PENDING, valor 150", pay)
s, pay2, _ = req("POST", f"/registrations/{reg_ana['id']}/payments", token=ANA, headers=kp, expect=[200, 201], label="replay idempotente pagamento", tmpl="/registrations/{registrationId}/payments")
check(pay2 and pay2.get("id") == pay["id"], "replay devolve mesmo pagamento", pay2)
s, pay3, _ = req("POST", f"/registrations/{reg_ana['id']}/payments", token=ANA, headers=key(), expect=[200, 201, 409], label="pagamento com nova chave (mesma inscrição)", tmpl="/registrations/{registrationId}/payments")
check(s == 409 or (pay3 and pay3.get("id") == pay["id"]), "não duplica pagamento por inscrição", pay3)
PID = pay["id"]
req("PATCH", f"/payments/{PID}/simulate/paid", token=ANA, expect=403, label="participante confirma próprio pagamento -> 403", tmpl="/payments/{paymentId}/simulate/paid")
req("PATCH", f"/payments/{PID}/simulate/paid", token=ARWEG, expect=[200, 403], label="ARWEG dono confirma pagamento (política)", tmpl="/payments/{paymentId}/simulate/paid")
s, conf, _ = req("PATCH", f"/payments/{PID}/simulate/paid", token=ADMIN, expect=200, label="ADMIN confirma pagamento", tmpl="/payments/{paymentId}/simulate/paid")
check(conf and conf["status"] == "PAID", "pagamento PAID", conf)
req("PATCH", f"/payments/{PID}/simulate/paid", token=ADMIN, expect=200, label="confirmação repetida idempotente", tmpl="/payments/{paymentId}/simulate/paid")
req("PATCH", f"/payments/{PID}/simulate/declined", token=ADMIN, expect=[400, 409, 422], label="PAID -> DECLINED inválido", tmpl="/payments/{paymentId}/simulate/declined")
_, r_after, _ = req("GET", f"/workshops/{PW}/registrations/me", token=ANA, expect=200, label="inscrição após pagamento", tmpl="/workshops/{workshopId}/registrations/me")
check(r_after["status"] == "CONFIRMED" and r_after["paymentStatus"] == "PAID", "inscrição CONFIRMED/PAID", r_after)
_, wp, _ = req("GET", f"/arweg/workshops/{PW}/payments", token=ARWEG, expect=200, label="ARWEG lista pagamentos", tmpl="/arweg/workshops/{workshopId}/payments")
req("GET", f"/arweg/workshops/{PW}/payments", token=ANA, expect=[403, 404], label="participante lista pagamentos -> 403", tmpl="/arweg/workshops/{workshopId}/payments")
_, nots, _ = req("GET", "/notifications?size=20", token=ANA, expect=200, label="notificações Ana", tmpl="/notifications")
titles = " | ".join(n.get("title", "") for n in nots.get("content", []))
check(re.search(r"pagamento|confirmad", titles, re.I), "notificação de pagamento confirmado", titles[:300])

print("\n=== GRUPO / CHAT ===")
if PG:
    req("GET", f"/groups/{PG}", token=ANA, expect=200, label="pago: acesso ao grupo liberado", tmpl="/groups/{groupId}")
    km = key()
    s, msg, _ = req("POST", f"/groups/{PG}/messages", token=ANA, headers=km, body={"content": "Olá turma!"}, expect=201, label="enviar mensagem", tmpl="/groups/{groupId}/messages")
    s, msg2, _ = req("POST", f"/groups/{PG}/messages", token=ANA, headers=km, body={"content": "Olá turma!"}, expect=[200, 201], label="replay mensagem", tmpl="/groups/{groupId}/messages")
    check(msg2 and msg2.get("id") == msg["id"], "mensagem não duplicada", msg2)
    req("POST", f"/groups/{PG}/messages", token=ANA, body={"content": ""}, expect=400, label="mensagem vazia -> 400", tmpl="/groups/{groupId}/messages")
    req("POST", f"/groups/{PG}/messages", token=ANA, body={"content": "x" * 5000}, expect=400, label="mensagem gigante -> 400", tmpl="/groups/{groupId}/messages")
    req("PATCH", f"/groups/{PG}/messages/{msg['id']}", token=ANA, body={"content": "Olá turma! (editado)"}, expect=200, label="editar mensagem", tmpl="/groups/{groupId}/messages/{messageId}")
    req("PATCH", f"/groups/{PG}/messages/{msg['id']}", token=ARWEG, body={"content": "hack"}, expect=[403, 404], label="ARWEG edita msg alheia -> 403/404", tmpl="/groups/{groupId}/messages/{messageId}")
    req("GET", f"/groups/{PG}/messages?size=1", token=ANA, expect=200, label="histórico paginado", tmpl="/groups/{groupId}/messages")
    _, page1, _ = req("GET", f"/groups/{PG}/messages?size=1", token=ANA)
    if isinstance(page1, dict) and page1.get("nextCursor"):
        req("GET", f"/groups/{PG}/messages?size=1&cursor={page1['nextCursor']}", token=ANA, expect=200, label="histórico cursor", tmpl="/groups/{groupId}/messages")
    req("GET", f"/groups/{PG}/messages", token=BRUNO, expect=[403, 404], label="lista de espera não lê chat", tmpl="/groups/{groupId}/messages")
    req("DELETE", f"/groups/{PG}/messages/{msg['id']}", token=ARWEG, expect=[200, 204], label="ARWEG criador modera (delete)", tmpl="/groups/{groupId}/messages/{messageId}")
    req("GET", f"/groups/{PG}", token=ADMIN, expect=200, label="ADMIN acessa qualquer grupo", tmpl="/groups/{groupId}")
    if OTHER_ARWEG:
        req("GET", f"/groups/{PG}", token=OTHER_ARWEG, expect=[403, 404], label="ARWEG não-dono sem acesso ao grupo", tmpl="/groups/{groupId}")
req("GET", "/groups", token=ANA, expect=200, label="participante lista grupos", tmpl="/groups")

print("\n=== CANCELAMENTO / REEMBOLSO / PROMOÇÃO ===")
kc = key()
s, canc, _ = req("PATCH", f"/registrations/{reg_ana['id']}/cancel", token=ANA, headers=kc, expect=200, label="Ana cancela (>48h) -> reembolso", tmpl="/registrations/{registrationId}/cancel")
check(canc and canc["status"] in ("CANCELLED", "REFUNDED") and canc["paymentStatus"] == "REFUNDED", "cancelamento >48h = REFUNDED", canc)
req("PATCH", f"/registrations/{reg_ana['id']}/cancel", token=ANA, headers=kc, expect=200, label="replay cancelamento idempotente", tmpl="/registrations/{registrationId}/cancel")
req("PATCH", f"/registrations/{reg_ana['id']}/cancel", token=ANA, headers=key(), expect=[400, 409, 422], label="cancelar de novo (nova chave) -> conflito", tmpl="/registrations/{registrationId}/cancel")
req("PATCH", f"/registrations/{reg_bruno['id']}/cancel", token=ANA, headers=key(), expect=[403, 404], label="cancelar inscrição alheia -> 403/404", tmpl="/registrations/{registrationId}/cancel")
_, rb, _ = req("GET", f"/workshops/{PW}/registrations/me", token=BRUNO, expect=200, label="Bruno após cancelamento", tmpl="/workshops/{workshopId}/registrations/me")
check(rb["status"] == "PENDING" and rb.get("waitingListPosition") is None, "Bruno promovido para PENDING", rb)
s, payb, _ = req("POST", f"/registrations/{reg_bruno['id']}/payments", token=BRUNO, headers=key(), expect=201, label="Bruno cria pagamento", tmpl="/registrations/{registrationId}/payments")
s, dec, _ = req("PATCH", f"/payments/{payb['id']}/simulate/declined", token=ADMIN, expect=200, label="ADMIN recusa pagamento", tmpl="/payments/{paymentId}/simulate/declined")
check(dec and dec["status"] == "DECLINED", "pagamento DECLINED", dec)
req("PATCH", f"/payments/{payb['id']}/simulate/paid", token=ADMIN, expect=[400, 409, 422], label="DECLINED -> PAID inválido", tmpl="/payments/{paymentId}/simulate/paid")
_, rb2, _ = req("GET", f"/workshops/{PW}/registrations/me", token=BRUNO, expect=[200, 404], label="Bruno após recusa", tmpl="/workshops/{workshopId}/registrations/me")
check(isinstance(rb2, dict) and rb2.get("status") in ("CANCELLED",) or rb2 is None or (isinstance(rb2, dict) and rb2.get("code")), "recusa libera a vaga (inscrição cancelada)", rb2)
s, rnew, _ = req("POST", f"/workshops/{PW}/registrations", token=BRUNO, headers=key(), expect=201, label="Bruno pode se reinscrever após recusa", tmpl="/workshops/{workshopId}/registrations")
check(rnew and rnew.get("status") == "PENDING", "vaga liberada ao recusar", rnew)
req("POST", f"/registrations/{reg_ana['id']}/payments", token=ANA, headers=key(), expect=[400, 409, 422], label="pagar inscrição cancelada -> erro", tmpl="/registrations/{registrationId}/payments")

# <48h, non-championship: no refund
s, rc, _ = req("POST", f"/workshops/{CW}/registrations", token=ANA, headers=key(), expect=201, label="Ana inscreve em cartão (<48h)", tmpl="/workshops/{workshopId}/registrations")
s, pc, _ = req("POST", f"/registrations/{rc['id']}/payments", token=ANA, headers=key(), expect=201, label="pagamento cartão", tmpl="/registrations/{registrationId}/payments")
req("PATCH", f"/payments/{pc['id']}/simulate/paid", token=ADMIN, expect=200, label="confirma cartão", tmpl="/payments/{paymentId}/simulate/paid")
s, cc, _ = req("PATCH", f"/registrations/{rc['id']}/cancel", token=ANA, headers=key(), expect=200, label="cancela <48h", tmpl="/registrations/{registrationId}/cancel")
check(cc and cc["status"] == "CANCELLED" and cc["paymentStatus"] == "PAID", "cancelamento <48h sem reembolso (CANCELLED/PAID)", cc)
# championship <48h: refund exception
s, rch, _ = req("POST", f"/workshops/{CHW}/registrations", token=ANA, headers=key(), expect=201, label="Ana inscreve em campeonato (<48h)", tmpl="/workshops/{workshopId}/registrations")
s, pch, _ = req("POST", f"/registrations/{rch['id']}/payments", token=ANA, headers=key(), expect=201, label="pagamento campeonato", tmpl="/registrations/{registrationId}/payments")
req("PATCH", f"/payments/{pch['id']}/simulate/paid", token=ADMIN, expect=200, label="confirma campeonato", tmpl="/payments/{paymentId}/simulate/paid")
s, cch, _ = req("PATCH", f"/registrations/{rch['id']}/cancel", token=ANA, headers=key(), expect=200, label="cancela campeonato <48h", tmpl="/registrations/{registrationId}/cancel")
check(cch and cch["paymentStatus"] == "REFUNDED", "campeonato <48h reembolsa (exceção)", cch)
# free registration
s, rf_, _ = req("POST", f"/workshops/{FW}/registrations", token=ANA, headers=key(), expect=201, label="Ana inscreve no gratuito", tmpl="/workshops/{workshopId}/registrations")
check(rf_["status"] == "CONFIRMED" and rf_["paymentStatus"] == "EXEMPT", "gratuito CONFIRMED/EXEMPT", rf_)
req("POST", f"/registrations/{rf_['id']}/payments", token=ANA, headers=key(), expect=[400, 409, 422], label="pagamento em inscrição gratuita -> erro", tmpl="/registrations/{registrationId}/payments")
req("POST", f"/workshops/{FW}/registrations", token=BRUNO, headers=key(), expect=201, label="Bruno inscreve no gratuito", tmpl="/workshops/{workshopId}/registrations")
req("PATCH", f"/payments/{uuid.uuid4()}/simulate/paid", token=ADMIN, expect=404, label="pagamento inexistente -> 404", tmpl="/payments/{paymentId}/simulate/paid")

print("\n=== HISTÓRICO / CALENDÁRIO / AVALIAÇÃO ===")
for f in ["FUTURE", "IN_PROGRESS", "COMPLETED", "CANCELLED", "WAITING_LIST"]:
    req("GET", f"/users/me/workshops/history?filter={f}", token=ANA, expect=200, label=f"histórico {f}", tmpl="/users/me/workshops/history")
req("GET", "/users/me/workshops/history?filter=XPTO", token=ANA, expect=400, label="histórico filtro inválido -> 400", tmpl="/users/me/workshops/history")
req("GET", f"/users/me/workshops/calendar?from={today}&to={today + dt.timedelta(days=60)}", token=ANA, expect=200, label="calendário", tmpl="/users/me/workshops/calendar")
req("GET", f"/users/me/workshops/calendar?from={today}&to={today - dt.timedelta(days=1)}", token=ANA, expect=400, label="calendário to<from -> 400", tmpl="/users/me/workshops/calendar")
ev = {"rating": 5, "contentRating": 5, "instructorRating": 4, "organizationRating": 5, "comment": "Ótimo"}
req("POST", f"/workshops/{FW}/evaluations", token=ANA, body=ev, expect=[400, 409, 422], label="avaliar workshop futuro -> bloqueado", tmpl="/workshops/{workshopId}/evaluations")
# find a completed workshop with confirmed registration for ana
_, hist, _ = req("GET", "/users/me/workshops/history?filter=COMPLETED&size=20", token=ANA)
done = [h for h in hist.get("content", [])] if isinstance(hist, dict) else []
if done:
    wid = done[0].get("workshopId") or done[0].get("workshop", {}).get("id") or done[0].get("id")
    s, _, _ = req("POST", f"/workshops/{wid}/evaluations", token=ANA, headers=key(), body=ev, expect=[201, 409], label="avaliar workshop concluído", tmpl="/workshops/{workshopId}/evaluations")
    req("POST", f"/workshops/{wid}/evaluations", token=ANA, headers=key(), body=ev, expect=409, label="avaliação duplicada -> 409", tmpl="/workshops/{workshopId}/evaluations")
    req("POST", f"/workshops/{wid}/evaluations", token=ANA, body={**ev, "rating": 9}, expect=[400, 409], label="nota fora de 1..5 -> 400", tmpl="/workshops/{workshopId}/evaluations")
    req("GET", f"/workshops/{wid}/evaluations", token=ADMIN, expect=200, label="admin lista avaliações", tmpl="/workshops/{workshopId}/evaluations")
    req("GET", f"/workshops/{wid}/evaluations/summary", token=ADMIN, expect=200, label="resumo avaliações", tmpl="/workshops/{workshopId}/evaluations/summary")
    req("GET", f"/workshops/{wid}/evaluations", token=ANA, expect=[403, 404], label="participante lista avaliações -> 403", tmpl="/workshops/{workshopId}/evaluations")
else:
    check(False, "Ana possui workshop COMPLETED para avaliar (seed)", hist)

print("\n=== POSTS / FEED ===")
s, post, _ = req("POST", "/posts", token=ARWEG, body={"title": f"Post E2E {SUF}", "content": "conteúdo", "workshopId": FW, "categoryId": CAT}, expect=201, label="ARWEG cria post", tmpl="/posts")
req("POST", "/posts", token=ANA, body={"title": "x", "content": "y"}, expect=403, label="participante cria post -> 403", tmpl="/posts")
req("PUT", f"/posts/{post['id']}", token=ARWEG, body={"title": f"Post E2E {SUF}!", "content": "conteúdo 2"}, expect=200, label="editar post", tmpl="/posts/{id}")
if OTHER_ARWEG:
    req("PUT", f"/posts/{post['id']}", token=OTHER_ARWEG, body={"title": "x", "content": "y"}, expect=[403, 404], label="ARWEG não-dono edita post -> 403", tmpl="/posts/{id}")
req("PUT", f"/posts/{post['id']}/like", token=ANA, expect=[400, 404, 409], label="curtir post DRAFT bloqueado", tmpl="/posts/{id}/like")
req("PATCH", f"/posts/{post['id']}/publish", token=ARWEG, expect=200, label="publicar post", tmpl="/posts/{id}/publish")
req("PUT", f"/posts/{post['id']}/like", token=ANA, expect=[200, 204], label="curtir", tmpl="/posts/{id}/like")
req("PUT", f"/posts/{post['id']}/like", token=ANA, expect=[200, 204], label="curtir de novo (idempotente)", tmpl="/posts/{id}/like")
req("DELETE", f"/posts/{post['id']}/like", token=ANA, expect=[200, 204], label="descurtir", tmpl="/posts/{id}/like")
kcm = key()
s, cm, _ = req("POST", f"/posts/{post['id']}/comments", token=ANA, headers=kcm, body={"content": "Muito bom"}, expect=201, label="comentar", tmpl="/posts/{id}/comments")
s, cm2, _ = req("POST", f"/posts/{post['id']}/comments", token=ANA, headers=kcm, body={"content": "Muito bom"}, expect=[200, 201], label="replay comentário", tmpl="/posts/{id}/comments")
check(cm2 and cm2.get("id") == cm["id"], "comentário não duplicado", cm2)
req("GET", f"/posts/{post['id']}/comments", token=BRUNO, expect=200, label="listar comentários", tmpl="/posts/{id}/comments")
req("PATCH", f"/posts/{post['id']}/comments/{cm['id']}", token=BRUNO, body={"content": "hack"}, expect=[403, 404], label="editar comentário alheio -> 403/404", tmpl="/posts/{postId}/comments/{commentId}")
req("PATCH", f"/posts/{post['id']}/comments/{cm['id']}", token=ANA, body={"content": "Muito bom!"}, expect=200, label="editar comentário", tmpl="/posts/{postId}/comments/{commentId}")
req("DELETE", f"/posts/{post['id']}/comments/{cm['id']}", token=ARWEG, expect=204, label="dono do post apaga comentário", tmpl="/posts/{postId}/comments/{commentId}")
_, feed, _ = req("GET", "/posts/feed?size=100", token=ANA, expect=200, label="feed", tmpl="/posts/feed")
check(any(p["id"] == post["id"] for p in feed.get("content", [])), "post publicado aparece no feed", "")
req("GET", f"/posts/feed?updatedAfter={upd_after}", token=ANA, expect=200, label="feed incremental", tmpl="/posts/feed")
req("GET", "/posts/managed", token=ARWEG, expect=200, label="posts gerenciados ARWEG", tmpl="/posts/managed")
req("GET", "/posts/managed", token=ANA, expect=403, label="posts gerenciados participante -> 403", tmpl="/posts/managed")
s, p2, _ = req("POST", "/posts", token=ARWEG, body={"title": f"Agendado {SUF}", "content": "c"}, expect=201, label="cria post p/ agendar", tmpl="/posts")
req("PATCH", f"/posts/{p2['id']}/schedule", token=ARWEG, body={"scheduledAt": sch}, expect=200, label="agendar post", tmpl="/posts/{id}/schedule")
req("PATCH", f"/posts/{p2['id']}/schedule", token=ARWEG, body={"scheduledAt": "2000-01-01T00:00:00Z"}, expect=[400, 409, 422], label="agendar no passado -> 400", tmpl="/posts/{id}/schedule")
req("PATCH", f"/posts/{post['id']}/archive", token=ARWEG, expect=200, label="arquivar post", tmpl="/posts/{id}/archive")
_, feed2, _ = req("GET", "/posts/feed?size=50", token=ANA)
check(not any(p["id"] == post["id"] for p in feed2.get("content", [])), "post arquivado sai do feed", "")

print("\n=== NOTIFICAÇÕES ===")
s, nn, _ = req("POST", "/arweg/notifications", token=ARWEG, body={"userIds": [me["ana"]["id"]], "title": "Aviso E2E", "message": "Teste"}, expect=[200, 201, 202], label="ARWEG envia notificação manual", tmpl="/arweg/notifications")
req("POST", "/arweg/notifications", token=ANA, body={"userIds": [me["bruno"]["id"]], "title": "spam", "message": "x"}, expect=403, label="participante envia notificação -> 403", tmpl="/arweg/notifications")
_, nl, _ = req("GET", "/notifications?size=5", token=ANA, expect=200, label="central de notificações", tmpl="/notifications")
nid = nl["content"][0]["id"]
req("PATCH", f"/notifications/{nid}/read", token=ANA, expect=[200, 204], label="marcar lida", tmpl="/notifications/{id}/read")
req("PATCH", f"/notifications/{nid}/read", token=BRUNO, expect=[403, 404], label="ler notificação alheia -> 404", tmpl="/notifications/{id}/read")
req("PATCH", "/notifications/read-all", token=ANA, expect=[200, 204], label="marcar todas", tmpl="/notifications/read-all")
req("GET", f"/notifications?updatedAfter={upd_after}", token=ANA, expect=200, label="notificações incremental", tmpl="/notifications")
s, dev, _ = req("POST", "/notification-devices", token=ANA, body={"token": f"ExponentPushToken[e2e{SUF}]", "platform": "ANDROID"}, expect=[200, 201], label="registrar device push", tmpl="/notification-devices")
if isinstance(dev, dict) and dev.get("id"):
    req("DELETE", f"/notification-devices/{dev['id']}", token=BRUNO, expect=[403, 404], label="remover device alheio -> 404", tmpl="/notification-devices/{id}")
    req("DELETE", f"/notification-devices/{dev['id']}", token=ANA, expect=204, label="remover device", tmpl="/notification-devices/{id}")

print("\n=== ADMINISTRAÇÃO ARWEG ===")
_, parts, _ = req("GET", f"/arweg/workshops/{FW}/participants", token=ARWEG, expect=200, label="participantes", tmpl="/arweg/workshops/{workshopId}/participants")
req("GET", f"/arweg/workshops/{FW}/participants?registrationStatus=CONFIRMED&paymentStatus=EXEMPT", token=ARWEG, expect=200, label="participantes filtrados", tmpl="/arweg/workshops/{workshopId}/participants")
req("GET", f"/arweg/workshops/{FW}/participants", token=ANA, expect=403, label="participante vê lista -> 403", tmpl="/arweg/workshops/{workshopId}/participants")
if OTHER_ARWEG:
    req("GET", f"/arweg/workshops/{FW}/participants", token=OTHER_ARWEG, expect=[403, 404], label="ARWEG não-dono vê participantes -> 403/404", tmpl="/arweg/workshops/{workshopId}/participants")
pc_ = parts.get("content", [])
ids = [p.get("registrationId") or p.get("id") for p in pc_]
if ids:
    req("PATCH", f"/arweg/workshops/{FW}/attendance", token=ARWEG, body={"updates": [{"registrationId": i, "status": "ATTENDED"} for i in ids]}, expect=200, label="presença em lote", tmpl="/arweg/workshops/{workshopId}/attendance")
    req("PATCH", f"/arweg/workshops/{FW}/attendance", token=ARWEG, body={"updates": [{"registrationId": ids[0], "status": "ATTENDED"}] * 2}, expect=[400, 409], label="presença IDs duplicados -> 400/409", tmpl="/arweg/workshops/{workshopId}/attendance")
    req("PATCH", f"/arweg/workshops/{FW}/attendance", token=ARWEG, body={"updates": [{"registrationId": rnew["id"], "status": "ATTENDED"}]}, expect=[400, 404, 409, 422], label="presença de inscrição de outro workshop -> erro", tmpl="/arweg/workshops/{workshopId}/attendance")
for fmt in ["CSV", "XLSX"]:
    s, data, hh = req("GET", f"/arweg/workshops/{FW}/participants/export?format={fmt}", token=ARWEG, raw=True, expect=200, label=f"exportar {fmt}", tmpl="/arweg/workshops/{workshopId}/participants/export")
    check(len(data) > 20, f"export {fmt} não vazio", len(data))
req("GET", "/arweg/dashboard", token=ARWEG, expect=200, label="dashboard ARWEG", tmpl="/arweg/dashboard")
req("GET", "/arweg/dashboard", token=ADMIN, expect=200, label="dashboard ADMIN", tmpl="/arweg/dashboard")
req("GET", "/arweg/dashboard", token=ANA, expect=403, label="dashboard participante -> 403", tmpl="/arweg/dashboard")

print("\n=== ADMIN: MÉTRICAS / AUDITORIA ===")
req("GET", "/admin/metrics/workshops", token=ADMIN, expect=200, label="métricas workshops", tmpl="/admin/metrics/workshops")
req("GET", "/admin/metrics/posts", token=ADMIN, expect=200, label="métricas posts", tmpl="/admin/metrics/posts")
req("GET", "/admin/metrics/posts", token=ARWEG, expect=403, label="ARWEG métricas posts -> 403", tmpl="/admin/metrics/posts")
req("GET", "/admin/metrics/workshops?createdFrom=2026-10-10T00:00:00Z&createdTo=2026-10-01T00:00:00Z", token=ADMIN, expect=400, label="métricas intervalo inválido -> 400", tmpl="/admin/metrics/workshops")
_, aud, _ = req("GET", "/admin/audit?entity=WORKSHOP&size=5", token=ADMIN, expect=200, label="auditoria", tmpl="/admin/audit")
req("GET", "http://localhost:8082/actuator/metrics", token=ARWEG, expect=403, label="actuator/metrics ARWEG -> 403")
req("GET", "http://localhost:8082/actuator/metrics", token=ADMIN, expect=200, label="actuator/metrics ADMIN")

print("\n=== CICLO DE VIDA FINAL ===")
req("PATCH", f"/workshops/{CW}/close", token=ARWEG, expect=200, label="fechar workshop", tmpl="/workshops/{id}/close")
req("PATCH", f"/workshops/{CW}/archive", token=ARWEG, expect=200, label="arquivar fechado", tmpl="/workshops/{id}/archive")
req("PATCH", f"/workshops/{CHW}/cancel", token=ARWEG, expect=200, label="cancelar workshop", tmpl="/workshops/{id}/cancel")
req("PATCH", f"/workshops/{CHW}/publish", token=ARWEG, expect=[400, 409, 422], label="republicar cancelado -> inválido", tmpl="/workshops/{id}/publish")
# Workshop cancellation with active paid registration: what happens to payment?
s, rp, _ = req("POST", f"/workshops/{PW}/registrations", token=ANA, headers=key(), expect=201, label="Ana reinscreve no pago (após cancelar)", tmpl="/workshops/{workshopId}/registrations")
_, regs_before_cancel, _ = req("GET", f"/workshops/{PW}/registrations?size=50", token=ARWEG)
s, pb, _ = req("POST", f"/registrations/{rnew['id']}/payments", token=BRUNO, headers=key(), expect=201, label="Bruno paga nova inscrição", tmpl="/registrations/{registrationId}/payments")
req("PATCH", f"/payments/{pb['id']}/simulate/paid", token=ADMIN, expect=200, label="confirma Bruno", tmpl="/payments/{paymentId}/simulate/paid")
req("PATCH", f"/workshops/{PW}/cancel", token=ARWEG, expect=200, label="organizador cancela workshop pago com inscritos", tmpl="/workshops/{id}/cancel")
_, rbx, _ = req("GET", f"/workshops/{PW}/registrations/me", token=BRUNO)
check(isinstance(rbx, dict) and rbx.get("paymentStatus") == "REFUNDED", "cancelar workshop reembolsa inscritos pagos", rbx)

print("\n=== COBERTURA ===")
spec = json.load(urllib.request.urlopen("http://localhost:8082/v3/api-docs"))
if spec:
    allops = {(m.upper(), p.replace("/api/v1", "")) for p, ops in spec["paths"].items() for m in ops}
    missing = sorted(allops - COVERED)
    print(f"rotas cobertas: {len(allops & COVERED)}/{len(allops)}")
    for m in missing:
        print("  NÃO COBERTA:", *m)

fails = [r for r in RESULTS if not r[0]]
slow = sorted([r for r in RESULTS if r[4] > 500], key=lambda r: -r[4])[:10]
print(f"\nTOTAL {len(RESULTS)} | PASS {len(RESULTS) - len(fails)} | FAIL {len(fails)}")
for f in fails:
    print("FAIL:", f[1], "status", f[2], "expected", f[3], "->", str(f[5])[:400])
print("Mais lentas:", [(r[1], r[4]) for r in slow])
