import mc from "minecraft-protocol";

const [
    host = "127.0.0.1",
    portText = "25565",
    scenario = "idle",
    countText = "1",
    durationText = "20",
] = process.argv.slice(2);
const port = Number.parseInt(portText, 10);
const count = Number.parseInt(countText, 10);
const durationSeconds = Number.parseInt(durationText, 10);

if (!Number.isInteger(port) || port <= 0) {
    throw new Error(`Invalid port: ${portText}`);
}
if (!Number.isInteger(count) || count <= 0) {
    throw new Error(`Invalid player count: ${countText}`);
}
if (!Number.isInteger(durationSeconds) || durationSeconds <= 0) {
    throw new Error(`Invalid duration: ${durationText}`);
}

const clients = [];
let loggedIn = 0;
let hadError = false;
const movementFlags = { onGround: true, hasHorizontalCollision: false };

function connect(index) {
    const client = mc.createClient({
        host,
        port,
        username: `SignLensP${index.toString().padStart(3, "0")}`,
        version: "26.2",
        auth: "offline",
    });
    client.once("login", () => {
        loggedIn++;
    });
    client.once("error", error => {
        hadError = true;
        console.error(`client ${index}: ${error.message}`);
    });
    clients.push(client);
}

for (let index = 0; index < count; index++) {
    connect(index);
}

let angle = 0;
const movementInterval = scenario === "churn" ? 100 : 250;
const movementTimer = scenario === "idle"
    ? null
    : setInterval(() => {
        angle = (angle + (scenario === "churn" ? 7 : 2)) % 360;
        for (const [index, client] of clients.entries()) {
            if (!client.state || client.state !== mc.states.PLAY) {
                continue;
            }
            client.write("position_look", {
                x: 0.5,
                y: 300.0,
                z: 0.5,
                yaw: angle + index % 5,
                pitch: 0.0,
                flags: movementFlags,
            });
        }
    }, movementInterval);

const finish = () => {
    if (movementTimer !== null) {
        clearInterval(movementTimer);
    }
    for (const client of clients) {
        client.end("performance probe complete");
    }
    console.log(`Performance clients: ${loggedIn}/${count} logged in; scenario=${scenario}`);
    process.exitCode = hadError || loggedIn !== count ? 1 : 0;
};

// The server probe has a five-second warm-up before its measurement window.
// Keep clients connected through that warm-up and the complete sample.
setTimeout(finish, durationSeconds * 1000 + 10_000);
