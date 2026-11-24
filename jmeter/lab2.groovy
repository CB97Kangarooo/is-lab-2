import groovy.json.JsonOutput
import groovy.json.JsonSlurper
import java.net.CookieManager
import java.net.CookiePolicy
import java.net.URI
import java.net.URLEncoder
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration
import java.util.concurrent.ConcurrentHashMap

def phase = Parameters
int worker = ctx.threadNum
def run = props.getProperty('run', 'lab2')
assert run ==~ /[a-zA-Z0-9-]+/
int iteration = vars.getIteration()
def prefix = "lab2-$run-$iteration"
def root = props.getProperty('baseUrl', 'http://127.0.0.1:8180/lab-is').replaceAll('/+$', '')
def password = System.getenv('LAB2_TEST_PASSWORD')
def stateKey = "lab2.results.$prefix"
def trace = []

def newClient = {
    HttpClient.newBuilder()
        .cookieHandler(new CookieManager(null, CookiePolicy.ACCEPT_ORIGINAL_SERVER))
        .connectTimeout(Duration.ofSeconds(5)).build()
}
def call = { client, method, path, payload = null, multipart = false ->
    def builder = HttpRequest.newBuilder(URI.create("$root/api$path"))
        .timeout(Duration.ofSeconds(15)).header('Accept', 'application/json')
    def body = HttpRequest.BodyPublishers.noBody()
    if (multipart) {
        def boundary = 'lab2-jmeter-' + UUID.randomUUID()
        builder.header('Content-Type', "multipart/form-data; boundary=$boundary")
        body = HttpRequest.BodyPublishers.ofString(
            "--$boundary\r\nContent-Disposition: form-data; name=\"file\"; filename=\"heroes.json\"\r\n" +
            "Content-Type: application/json\r\n\r\n$payload\r\n--$boundary--\r\n")
    } else if (payload != null) {
        builder.header('Content-Type', 'application/json')
        body = HttpRequest.BodyPublishers.ofString(JsonOutput.toJson(payload))
    }
    def response = client.send(builder.method(method, body).build(), HttpResponse.BodyHandlers.ofString())
    int code = response.statusCode()
    trace << [method: method, path: path, code: code]
    def result = null
    if (response.body() && response.headers().firstValue('Content-Type').orElse('').contains('application/json')) {
        result = new JsonSlurper().parseText(response.body())
    }
    [code: code, body: result]
}
def expect = { result, int code ->
    assert result.code == code : "$phase expected $code, got $result"
    result.body
}
def login = { client, username ->
    assert password : 'Set LAB2_TEST_PASSWORD before launching JMeter'
    expect(call(client, 'POST', '/auth/login', [username: username, password: password]), 200)
}
def hero = { name, int speed = 42 ->
    [name: name, coordinates: [x: 1, y: 2], realHero: true, hasToothpick: false,
     mood: 'GLOOM', impactSpeed: speed]
}
def upload = { client, rows ->
    call(client, 'POST', '/imports', rows instanceof String ? rows : JsonOutput.toJson(rows), true)
}
def deletePath = { fighter -> "/human-beings/$fighter.id?version=$fighter.version" }
def update = { client, fighter, name, int speed ->
    call(client, 'PUT', "/human-beings/$fighter.id", hero(name, speed) + [version: fighter.version])
}
def matching = { client, name ->
    def path = '/human-beings?limit=10&filter.name=' + URLEncoder.encode(name.toString(), 'UTF-8')
    expect(call(client, 'GET', path), 200).items.findAll { it.name.strip().equalsIgnoreCase(name.toString()) }
}
def history = { client, watchedIds ->
    def rows = []
    int offset = 0
    while (true) {
        def page = expect(call(client, 'GET', "/imports?offset=$offset"), 200)
        rows.addAll(page.items)
        if (!page.hasMore || (rows*.id).containsAll(watchedIds)) return rows
        offset += page.pageSize as int
    }
}
def clients = vars.getObject('lab2.client')
def username = props.getProperty(worker == 0 ? 'userA' : 'userB', worker == 0 ? 'lab_a' : 'lab_b')
def state = props.get(stateKey)

try {
    switch (phase) {
        case 'prepare':
            if (clients == null) {
                clients = newClient()
                login(clients, username)
                vars.putObject('lab2.client', clients)
            }
            if (worker == 0) {
                state = new ConcurrentHashMap()
                def create = { suffix ->
                    def item = expect(call(clients, 'POST', '/human-beings', hero("$prefix-$suffix")), 201)
                    // Materialize the fields before sharing them with another thread.
                    [id: item.id, name: item.name, version: item.version].asImmutable()
                }
                def crud = create('crud')
                def edited = expect(update(clients, crud, crud.name, 43), 200)
                assert edited.impactSpeed == 43 && edited.version > crud.version
                expect(call(clients, 'DELETE', deletePath(edited)), 204)
                expect(call(clients, 'GET', "/human-beings/$crud.id"), 404)
                state.put('edit', create('edit'))
                state.put('delete', create('delete'))
                state.put('rename0', create('rename-0'))
                state.put('rename1', create('rename-1'))
                def existing = create('existing')
                def carTotal = expect(call(clients, 'GET', '/cars?limit=1'), 200).total
                def conflict = expect(upload(clients, [
                    hero("$prefix-rollback") + [car: [name: "$prefix-rollback-car", cool: true]],
                    hero("  " + existing.name.toUpperCase(Locale.ROOT) + "  ")
                ]), 409)
                assert conflict.fieldErrors.containsKey('Запись 2.name')
                assert matching(clients, "$prefix-rollback").isEmpty()
                assert expect(call(clients, 'GET', '/cars?limit=1'), 200).total == carTotal
                def malformed = expect(upload(clients, '['), 400)
                def dup = expect(upload(clients, [hero("$prefix-file-dup"), hero("$prefix-file-dup")]), 409)
                assert matching(clients, "$prefix-file-dup").isEmpty()
                state.put('failures', [conflict.id, malformed.id, dup.id])
                props.put(stateKey, state)
            }
            break
        case 'edit':
            assert state
            def item = state.get('edit')
            state.put("edit$worker".toString(), update(clients, item, item.name, 51 + worker).code)
            break
        case 'delete':
            assert state
            def item = state.get('delete')
            def result = worker == 0 ? update(clients, item, item.name, 61)
                                     : call(clients, 'DELETE', deletePath(item))
            state.put("delete$worker".toString(), result.code)
            break
        case 'create':
            assert state
            def name = "$prefix-create"
            def variant = worker == 0 ? name : '  ' + name.toUpperCase(Locale.ROOT) + '  '
            state.put("create$worker".toString(), call(clients, 'POST', '/human-beings', hero(variant)).code)
            break
        case 'rename':
            assert state
            def item = state.get("rename$worker".toString())
            state.put("renameResult$worker".toString(), update(clients, item, "$prefix-rename-target", 72).code)
            break
        case 'mixed':
            assert state
            def name = "$prefix-mixed"
            def result = worker == 0 ? call(clients, 'POST', '/human-beings', hero(name))
                                     : upload(clients, [hero(name)])
            state.put("mixed$worker".toString(), result.code)
            if (worker == 1) {
                assert result.body?.id
                state.put('mixedImport', result.body.id)
                def success = expect(upload(clients, [
                    hero("$prefix-import-one") + [car: [name: "$prefix-car", cool: true]],
                    hero("$prefix-import-two")
                ]), 201)
                assert success.addedCount == 2
                state.put('success', success.id)
            }
            break
        case 'verify':
            if (worker != 0) break
            assert state
            def editCodes = [state.get('edit0'), state.get('edit1')]
            def deleteCodes = [state.get('delete0'), state.get('delete1')]
            def createCodes = [state.get('create0'), state.get('create1')]
            def renameCodes = [state.get('renameResult0'), state.get('renameResult1')]
            def mixedCodes = [state.get('mixed0'), state.get('mixed1')]
            def createCount = matching(clients, "$prefix-create").size()
            def renameCount = matching(clients, "$prefix-rename-target").size()
            def mixedCount = matching(clients, "$prefix-mixed").size()
            def outcome = "edit=$editCodes delete=$deleteCodes create=$createCodes/$createCount " +
                          "rename=$renameCodes/$renameCount mixed=$mixedCodes/$mixedCount"
            SampleResult.setResponseMessage(outcome)
            def checks = []
            if (editCodes.sort() != [200, 409]) checks << 'edit outcome'
            def edited = expect(call(clients, 'GET', "/human-beings/"+state.get('edit').id), 200)
            if (edited.impactSpeed != ([state.get('edit0'), state.get('edit1')].indexOf(200) + 51)) {
                checks << 'edit persisted wrong values'
            }
            def afterDelete = call(clients, 'GET', "/human-beings/"+state.get('delete').id)
            if (!(deleteCodes == [200, 409] && afterDelete.code == 200 && afterDelete.body.impactSpeed == 61)
                    && !(deleteCodes[0] in [404, 409] && deleteCodes[1] == 204 && afterDelete.code == 404)) {
                checks << 'update/delete outcome'
            }
            if (createCodes.sort() != [201, 409] || createCount != 1) checks << 'concurrent creates'
            if (renameCodes.sort() != [200, 409] || renameCount != 1) checks << 'concurrent renames'
            if (mixedCodes.sort() != [201, 409] || mixedCount != 1) checks << 'create/import race'
            def imported = matching(clients, "$prefix-import-one")
            assert imported.size() == 1 && imported[0].car?.name == "$prefix-car"
            assert matching(clients, "$prefix-import-two").size() == 1
            def ownIds = state.get('failures')
            def otherIds = [state.get('success'), state.get('mixedImport')]
            assert otherIds.every { it != null }
            def own = history(clients, ownIds)
            assert own.every { it.username == username }
            assert own.findAll { it.id in ownIds }.size() == ownIds.size()
            assert own.findAll { it.id in ownIds }.every { it.status == 'FAILED' && !it.containsKey('addedCount') }
            assert own.every { !(it.id in otherIds) }
            def other = vars.getObject('lab2.other')
            if (other == null) {
                other = newClient()
                login(other, props.getProperty('userB', 'lab_b'))
                vars.putObject('lab2.other', other)
            }
            def otherRows = history(other, otherIds)
            assert otherRows.every { it.username == props.getProperty('userB', 'lab_b') }
            assert otherRows.findAll { it.id in otherIds }.size() == otherIds.size()
            assert otherRows.every { !(it.id in ownIds) }
            def admin = vars.getObject('lab2.admin')
            if (admin == null) {
                admin = newClient()
                login(admin, props.getProperty('admin', 'lab_admin'))
                vars.putObject('lab2.admin', admin)
            }
            def all = history(admin, ownIds + otherIds)
            assert all.findAll { it.id in ownIds }.size() == ownIds.size()
            assert all.findAll { it.id in otherIds }.size() == otherIds.size()
            assert all.find { it.id == state.get('success') }?.addedCount == 2
            def mixedEntry = all.find { it.id == state.get('mixedImport') }
            if (state.get('mixed1') == 201) {
                assert mixedEntry.status == 'SUCCESS' && mixedEntry.addedCount == 1
            } else {
                assert mixedEntry.status == 'FAILED' && !mixedEntry.containsKey('addedCount')
            }
            if (checks) {
                SampleResult.setSuccessful(false)
                SampleResult.setResponseCode('500')
                SampleResult.setResponseMessage(outcome + ' failed: ' + checks.join(', '))
            }
            props.remove(stateKey)
            break
        default:
            throw new IllegalArgumentException("Unknown phase: $phase")
    }
} catch (Exception | AssertionError error) {
    SampleResult.setSuccessful(false)
    SampleResult.setResponseCode('500')
    SampleResult.setResponseMessage("$phase: " + (error.message ?: error.class.simpleName))
} finally {
    SampleResult.setResponseData(JsonOutput.toJson(trace), 'UTF-8')
}
