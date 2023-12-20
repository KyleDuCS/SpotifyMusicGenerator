# spotifyGenerator
This generates a spotify playlist for a user given some artists.

This has 2 files, a client file and a SpotUser file. 

The client file gathers the user input for the artists, the number of songs, and their identification. This allows the SpotUser file to convert that information into a playlist for the user. The client will provide their identification and it will create an API request. This allows for the playlist to be generated.

The SpotUser file is where the playlist is generated. This file contains a Spotify user's API request and it will take each artist the user gives and select their top 10 songs to add to a playlist. This playlist will be generated in Spotify and the user can then use it just like any other Spotify file.

Currently, this project only works within Java/an IDE but it should have the frame work to allow for a web-base app to be designed around this generator. 
