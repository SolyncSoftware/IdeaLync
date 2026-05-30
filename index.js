import { Client, Events, GatewayIntentBits } from 'discord.js';
import { DISCORD_TOKEN, ROLE_ID } from './config.json';
const client = new Client({
    intents: [GatewayIntentBits.Guilds, GatewayIntentBits.GuildMembers]
});

client.once(Events.ClientReady, (readyClient) => {
    console.log(`Ready! Logged in as ${readyClient.user.tag}`);
});

// updated member
client.on(Events.GuildMemberUpdate, (oldMember, newMember) => {
    if (!oldMember.roles.cache.has(ROLE_ID) && newMember.roles.cache.has(ROLE_ID)) {
        console.log(`Attempting to ban ${newMember.user.tag} (role added).`);
        newMember
            .ban({ reason: 'Banned user based on bot role' })
            .then(() => console.log(`Banned ${newMember.user.tag}`))
            .catch(console.error);
    }
});

// joins with role
client.on(Events.GuildMemberAdd, (member) => {
    if (member.roles.cache.has(ROLE_ID)) {
        console.log(`Attempting to ban ${member.user.tag} (joined with role).`);
        member
            .ban({ reason: 'Banned user based on bot role' })
            .then(() => console.log(`Banned ${member.user.tag}`))
            .catch(console.error);
    }
});

client.login(DISCORD_TOKEN);
