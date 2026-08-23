import mc from "minecraft-protocol";

const [host = "127.0.0.1", portText = "25565"] = process.argv.slice(2);
const port = Number.parseInt(portText, 10);

if (!Number.isInteger(port) || port <= 0) {
    throw new Error(`Invalid port: ${portText}`);
}

const client = mc.createClient({
    host,
    port,
    username: "SignLensProbe",
    version: "26.2",
    auth: "offline",
});

const actionBarPackets = [];
client.on("packet", (data, meta) => {
    if (meta.name === "action_bar") {
        actionBarPackets.push(JSON.stringify(data));
    }
});

const timeout = setTimeout(() => {
    client.end("integration timeout");
    process.exitCode = 1;
}, 15_000);

client.once("login", () => {
    console.log("Connected to Paper 26.2 as SignLensProbe");
    setTimeout(() => {
        const multilinePacket = actionBarPackets.find(packet =>
            packet.includes("123") && packet.includes("456") && packet.includes("\\n")
        );
        if (!multilinePacket) {
            console.error("No multiline 123/456 ActionBar packet was observed");
            process.exitCode = 1;
        } else {
            console.log("Observed multiline 123/456 ActionBar packet");
        }
        clearTimeout(timeout);
        client.end("integration probe complete");
    }, 8_000);
});

client.once("end", () => {
    clearTimeout(timeout);
});

client.once("error", (error) => {
    clearTimeout(timeout);
    console.error(error);
    process.exitCode = 1;
});
