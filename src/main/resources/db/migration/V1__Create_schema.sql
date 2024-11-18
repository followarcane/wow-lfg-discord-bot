CREATE TABLE discord_channels (
                                  id BIGINT AUTO_INCREMENT PRIMARY KEY,
                                  channel_id VARCHAR(255),
                                  channel_name VARCHAR(255),
                                  server_id BIGINT,
                                  character_name VARCHAR(255)
);

CREATE TABLE discord_servers (
                                 server_id VARCHAR(255) PRIMARY KEY,
                                 server_name VARCHAR(255),
                                 owner_id VARCHAR(255),
                                 user_id BIGINT
);

CREATE TABLE messages (
                          id BIGINT AUTO_INCREMENT PRIMARY KEY,
                          message_guild_id VARCHAR(255),
                          message_channel_id VARCHAR(255),
                          message_content VARCHAR(255),
                          timestamp BIGINT
);

CREATE TABLE users (
                       id BIGINT AUTO_INCREMENT PRIMARY KEY,
                       discord_id VARCHAR(255),
                       username VARCHAR(255),
                       global_name VARCHAR(255),
                       discriminator VARCHAR(255),
                       avatar VARCHAR(255),
                       banner VARCHAR(255),
                       banner_color VARCHAR(255),
                       locale VARCHAR(255)
);

CREATE TABLE user_settings (
                               id BIGINT AUTO_INCREMENT PRIMARY KEY,
                               language VARCHAR(255),
                               realm VARCHAR(255),
                               region VARCHAR(255),
                               user_id BIGINT,
                               server_id VARCHAR(255),
                               channel_id BIGINT
);

ALTER TABLE discord_channels ADD FOREIGN KEY (server_id) REFERENCES discord_servers(server_id);

ALTER TABLE discord_servers ADD FOREIGN KEY (user_id) REFERENCES users(id);

ALTER TABLE user_settings ADD FOREIGN KEY (user_id) REFERENCES users(id);

ALTER TABLE user_settings ADD FOREIGN KEY (server_id) REFERENCES discord_servers(server_id);

ALTER TABLE user_settings ADD FOREIGN KEY (channel_id) REFERENCES discord_channels(id);