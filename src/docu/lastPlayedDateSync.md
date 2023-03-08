# Watched sync via Trakt.tv and/or Kodi RPC.

- Our exchange format for Kodi and every other MediaCentre is the NFO file.
- When syncing the "watched" flag, we DO NEED to write the last played date.
- When writing a NFO we need to KEEP the existing lastPlayedDate.

Since we do not (and cannot) manage this date inside TMM, this works as following:

### UseCase 1:
Movie gets watched inside Kodi, and an "export NFO files" is triggered.
NFO file is written by Kodi, and every successive write from TMM will not alter the date (since it is always NULL on our side)

### UseCase 2:
Movie gets watched inside Kodi, and a "get watched from Kodi RPC" call is sent.
We get the date from Kodi, write this immediately into NFO, and NULL out the date again inside TMM.

### UseCase 3:
Movie is watched inside Trakt.tv, and a Trakt sync is triggered.
We get the date from Trakt, write this immediately into NFO, and NULL out the date again inside TMM.
Every remaining difference is then synced back to Trakt.

Since we cannot distinguish if the date changed, we basically always rewrite the NFO file.
(ok, or writer is smart enough, to at least do a conetents check, IF there are really some changes)


