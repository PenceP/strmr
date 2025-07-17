# Strmr

## Local Secrets Setup

To build and run this project, you must create a `secrets.properties` file in the project root with your API keys:

```
TRAKT_API_KEY=your_trakt_key_here
TMDB_READ_KEY=your_tmdb_key_here
OMDB_API_KEY=your_omdb_key_here
```

- This file is **not checked into git** (see `.gitignore`).
- You can obtain your own API keys from [Trakt](https://trakt.tv/oauth/applications) and [OMDb](https://www.omdbapi.com/apikey.aspx).

## Build

Once you have added your keys, build as usual:

```
./gradlew assembleDebug
```

## Security
- **Never check your API keys into version control.**
- Each developer/contributor must provide their own keys in `secrets.properties`. 