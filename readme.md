# BoardGameOnline

BoardGameOnline/BGO is an online multiplayer site which uses the Spring framework, designed to be versatile and extendable. It currently includes tic tac toe, checkers, and connect 4. It was made in 2020. An instance is hosted [here](https://bgo-games.uc.r.appspot.com/).

## Disclaimer

The online version uses a basic free plan and does not run properly. If you want to see this fully in action, you'll need to run it locally (or give me a nicer server).

## Run

The app uses gradle to startup. It expects a postgresql database.

## Config
Environment variables:

```bash
APP_DOMAIN: <location of site, used in email verification redirect>
DBC_DATABASE_URL: <database url>
JDBC_DATABASE_USERNAME: <database username>
JDBC_DATABASE_PASSWORD: <database password>
MAIL_HOST: <verification email host>
MAIL_USERNAME: <mail username>
MAIL_PASSWORD: <mail password>
```

## Contributing
This project is finished and here for archive purposes.