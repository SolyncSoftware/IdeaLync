import { Client, Events, GatewayIntentBits } from 'discord.js';

const {
    IDEALYNC_DISCORD_TOKEN = '', // let discord.js crash
    IDEALYNC_ROLE_ID
} = Bun.env;


if (!IDEALYNC_ROLE_ID) {
    throw new Error("IDEALYNC_ROLE_ID isn't set in environment variables.");
}

const client = new Client({
    intents: [GatewayIntentBits.Guilds, GatewayIntentBits.GuildMembers]
});

client.once(Events.ClientReady, (readyClient) => {
    console.log(`Ready! Logged in as ${readyClient.user.tag}`);
});

// updated member
client.on(Events.GuildMemberUpdate, (oldMember, newMember) => {
    if (!oldMember.roles.cache.has(IDEALYNC_ROLE_ID) && newMember.roles.cache.has(IDEALYNC_ROLE_ID)) {
        console.log(`Attempting to ban ${newMember.user.tag} (role added).`);
        newMember
            .ban({ reason: 'Banned user based on bot role' })
            .then(() => console.log(`Banned ${newMember.user.tag}`))
            .catch(console.error);
    }
});

// joins with role
client.on(Events.GuildMemberAdd, (member) => {
    if (member.roles.cache.has(IDEALYNC_ROLE_ID)) {
        console.log(`Attempting to ban ${member.user.tag} (joined with role).`);
        member
            .ban({ reason: 'Banned user based on bot role' })
            .then(() => console.log(`Banned ${member.user.tag}`))
            .catch(console.error);
    }
});

client.login(IDEALYNC_DISCORD_TOKEN);
