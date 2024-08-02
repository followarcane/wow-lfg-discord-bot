# **This project is an open-source service designed for use by [Azerite.app](https://azerite.app).** 

# WoW LFG Discord Bot

This project is a Discord bot that automatically posts World of Warcraft players' LFG (Looking for Group) listings to specific Discord channels. The bot fetches data from another project called [WowProgressDataCrawler](https://github.com/followarcane/WowProgressDataCrawler). This data includes the latest LFG listings of players, and the bot posts these listings to a specified Discord channel. The data is scraped from wowprogress.com and supplemented with additional information from raider.io and warcraftlogs.com.

## Features

- Fetch data from WowProgressDataCrawler API
- Post new data to a specific Discord channel
- Update data every 30 seconds
- Post messages as embeds
- Compare with previous data and only post new listings

## Data From

- wowprogress.com
- warcraftlogs.com
- raider.io

## Requirements

- Java 17+
- Spring Boot
- Discord Developer Account and bot token
- PostgreSQL database

## Setup

### Step 1: Clone the Project

```bash
git clone https://github.com/followarcane/wow-lfg-discord-bot.git
cd wow-lfg-discord-bot
```

### Step 2: Install Dependencies

```bash
./gradlew build
```

### Step 3: Configure the Application

Open the `src/main/resources/application.yml` file and configure the necessary settings.

```yaml
server:
  port: 8081

wow-api:
  username: your_wow_api_username
  password: your_wow_api_password

discord:
  bot:
    token: ${DISCORD_BOT_TOKEN}

spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/wowdiscordbot
    username: postgres
    password: pass
  jpa:
    hibernate:
      ddl-auto: create
  flyway:
    enabled: true
    clean-disabled: true
```

### Step 4: Set Environment Variables

Set your Discord bot token as an environment variable.

Linux/MacOS:
```bash
export DISCORD_BOT_TOKEN=your_discord_bot_token
```

Windows:
```cmd
set DISCORD_BOT_TOKEN=your_discord_bot_token
```

### Step 5: Database Setup

Set up your PostgreSQL database and update the database configuration in `application.yml`.

```sql
CREATE DATABASE wowdiscordbot;
```

### Step 6: Run the Project

```bash
./gradlew bootRun
```

## Usage

Once the bot is running, it will make a request to the [WowProgressDataCrawler](https://github.com/followarcane/WowProgressDataCrawler) API every 30 seconds and fetch the latest LFG listings. These listings are then posted to a specific Discord channel. The data is collected from wowprogress.com and enhanced with additional information from raider.io and warcraftlogs.com.
