/*
 * Reactive Ampache, a reactive Ampache library for Android
 * Copyright (C) 2016  Antonio Tari
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
*/
package com.antoniotari.reactiveampache.api;

import android.content.Context;

import java.io.IOException;
import java.util.List;

import com.antoniotari.reactiveampache.models.TagEntity;
import com.antoniotari.reactiveampache.models.TagsResponse;
import com.google.gson.Gson;

import com.antoniotari.reactiveampache.Exceptions.AmpacheApiException;
import com.antoniotari.reactiveampache.api.RawRequest.PlaylistType;
import com.antoniotari.reactiveampache.models.Album;
import com.antoniotari.reactiveampache.models.AlbumsResponse;
import com.antoniotari.reactiveampache.models.Artist;
import com.antoniotari.reactiveampache.models.ArtistsResponse;
import com.antoniotari.reactiveampache.models.BaseResponse;
import com.antoniotari.reactiveampache.models.HandshakeResponse;
import com.antoniotari.reactiveampache.models.PingResponse;
import com.antoniotari.reactiveampache.models.Playlist;
import com.antoniotari.reactiveampache.models.PlaylistsResponse;
import com.antoniotari.reactiveampache.models.Song;
import com.antoniotari.reactiveampache.models.SongsResponse;
import com.antoniotari.reactiveampache.utils.FileUtil;

import rx.Observable;
import rx.Observable.OnSubscribe;
import rx.Subscriber;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;
import rx.schedulers.Schedulers;

/**
 * Created by antonio tari on 2016-05-21.
 */
public enum AmpacheApi {
    INSTANCE;

    private static final String FILENAME_ARTISTS = "com.antoniotari.ampache.library.response.artists.json";
    private static final String FILENAME_ALBUMS = "com.antoniotari.ampache.library.response.albums.json";
    private static final String FILENAME_SONGS = "com.antoniotari.ampache.library.response.songs.json";

    private RawRequest mRawRequest;
    private Context mContext;

    public void initSession(Context context) {
        AmpacheSession.INSTANCE.init(context);
        mContext = context.getApplicationContext();
    }

    /**
     * initialize the ampache user, use this before making any other API call
     * @param ampacheUrl        url for the ampache server
     * @param ampacheUser       ampache user username
     * @param ampachePassword   ampache user password
     * @return                  an observable that will complete if the user is valid otherwise goes onError
     */
    public Observable<AmpacheSession> initUser(final String ampacheUrl, final String ampacheUser, final String ampachePassword) {
        return Observable.create(new OnSubscribe<AmpacheSession>() {

            @Override
            public void call(final Subscriber<? super AmpacheSession> subscriber) {
                try {
                    if (ampachePassword == null || ampachePassword.isEmpty()) {
                        throw new Exception("invalid password");
                    }
                    if (ampacheUser == null || ampacheUser.isEmpty()) {
                        throw new Exception("invalid user name");
                    }
                    if (ampacheUrl == null || ampacheUrl.isEmpty()) {
                        throw new Exception("invalid url");
                    }

                    String ampacheUrlMod = ampacheUrl;
                    if (!ampacheUrl.endsWith("/")) {
                        ampacheUrlMod = ampacheUrl + "/";
                    }

                    // initialize the session
                    AmpacheSession.INSTANCE.setAmpachePassword(ampachePassword);
                    AmpacheSession.INSTANCE.setAmpacheUrl(ampacheUrlMod);
                    AmpacheSession.INSTANCE.setAmpacheUser(ampacheUser);
                    // initialize raw request
                    mRawRequest = new RawRequest(ampacheUrlMod, ampacheUser, ampachePassword);
                    subscriber.onNext(AmpacheSession.INSTANCE);
                    subscriber.onCompleted();
                } catch (Exception e) {
                    subscriber.onError(e);
                }
            }
        });
    }

    /**
     * use this only if the use already logged in before
     */
    public Observable<AmpacheSession> initUser() {
        return initUser(AmpacheSession.INSTANCE.getAmpacheUrl(),
                AmpacheSession.INSTANCE.getAmpacheUser(),
                AmpacheSession.INSTANCE.getAmpachePassword());
    }

    private RawRequest getRawRequest() {
        if(mRawRequest == null){
            mRawRequest = new RawRequest(AmpacheSession.INSTANCE.getAmpacheUrl(),
                    AmpacheSession.INSTANCE.getAmpacheUser(),
                    AmpacheSession.INSTANCE.getAmpachePassword());
        }
        return mRawRequest;
    }

    /**
     * before making any API call must handshake with the server
     */
    public Observable<HandshakeResponse> handshake() {
        return Observable.create(new OnSubscribe<HandshakeResponse>() {

            @Override
            public void call(final Subscriber<? super HandshakeResponse> subscriber) {
                try {
                    HandshakeResponse handshakeResponse = getRawRequest().handshake();
                    if (handshakeResponse.getError() != null) throw new AmpacheApiException(handshakeResponse.getError());
                    AmpacheSession.INSTANCE.setHandshakeResponse(handshakeResponse);
                    subscriber.onNext(handshakeResponse);
                    subscriber.onCompleted();
                } catch (Exception e) {
                    subscriber.onError(e);
                }
            }
        })
                .retry(4)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread());
    }

    /**
     * get a list of all the artists
     */
    public Observable<List<Artist>> getArtists() {
        return Observable.create(new OnSubscribe<List<Artist>>() {

            @Override
            public void call(final Subscriber<? super List<Artist>> subscriber) {
                try {

                        ArtistsResponse artistsResponse = getRawRequest().getArtists(AmpacheSession.INSTANCE.getHandshakeResponse().getAuth());
                        if (artistsResponse.getError() != null)
                            throw new AmpacheApiException(artistsResponse.getError());


                    subscriber.onNext(artistsResponse.getArtists());
                    subscriber.onCompleted();
                } catch (Exception e) {
                    subscriber.onError(e);
                }
            }
        })
                .doOnError(doOnError)
                .retry(9)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread());
    }

    /**
     * get a list of all the albums for given artist
     */
    public Observable<List<Album>> getAlbumsFromArtist(final String artistId) {
        return Observable.create(new OnSubscribe<List<Album>>() {

            @Override
            public void call(final Subscriber<? super List<Album>> subscriber) {
                try {
                    AlbumsResponse albumsResponse =
                            getRawRequest().getAlbumsFromArtist(AmpacheSession.INSTANCE.getHandshakeResponse().getAuth(), artistId);
                    if (albumsResponse.getError()!=null) throw new AmpacheApiException(albumsResponse.getError());
                    subscriber.onNext(albumsResponse.getAlbums());
                    subscriber.onCompleted();
                } catch (Exception e) {
                    subscriber.onError(e);
                }
            }
        })
                .doOnError(doOnError)
                .retry(9)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread());
    }

    /**
     * get a list of all the albums
     */
    public Observable<List<Album>> getAlbums() {
        return Observable.create(new OnSubscribe<List<Album>>() {

            @Override
            public void call(final Subscriber<? super List<Album>> subscriber) {
                try {

                        AlbumsResponse albumsResponse = getRawRequest().getAlbums(AmpacheSession.INSTANCE.getHandshakeResponse().getAuth());
                        if (albumsResponse.getError() != null)
                            throw new AmpacheApiException(albumsResponse.getError());


                    subscriber.onNext(albumsResponse.getAlbums());
                    subscriber.onCompleted();
                } catch (Exception e) {
                    subscriber.onError(e);
                }
            }
        })
                .doOnError(doOnError)
                .retry(9)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread());
    }

    /**
     * get the album from the album id
     */
    public Observable<Album> getAlbumFromId(final String albumId) {
        return Observable.create(new OnSubscribe<Album>() {

            @Override
            public void call(final Subscriber<? super Album> subscriber) {
                try {
                    AlbumsResponse albumResponse = getRawRequest().getAlbumFromId(AmpacheSession.INSTANCE.getHandshakeResponse().getAuth(), albumId);
                    if (albumResponse.getError()!=null) throw new AmpacheApiException(albumResponse.getError());
                    subscriber.onNext(albumResponse.getAlbums().get(0));
                    subscriber.onCompleted();
                } catch (Exception e) {
                    subscriber.onError(e);
                }
            }
        })
                .doOnError(doOnError)
                .retry(9)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread());
    }

    /**
     * get artist from artist id
     */
    public Observable<Artist> getArtistFromId(final String artistId) {
        return Observable.create(new OnSubscribe<Artist>() {

            @Override
            public void call(final Subscriber<? super Artist> subscriber) {
                try {
                    ArtistsResponse artistsResponse = getRawRequest().getArtistFromId(AmpacheSession.INSTANCE.getHandshakeResponse().getAuth(), artistId);
                    if (artistsResponse.getError()!=null) throw new AmpacheApiException(artistsResponse.getError());
                    subscriber.onNext(artistsResponse.getArtists().get(0));
                    subscriber.onCompleted();
                } catch (Exception e) {
                    subscriber.onError(e);
                }
            }
        })
                .doOnError(doOnError)
                .retry(9)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread());
    }

    /**
     * get a list of all the songs
     */
    public Observable<List<Song>> getSongs() {
        return Observable.create(new OnSubscribe<List<Song>>() {

            @Override
            public void call(final Subscriber<? super List<Song>> subscriber) {
                try {

                        SongsResponse songsResponse = getRawRequest().getSongs(AmpacheSession.INSTANCE.getHandshakeResponse().getAuth());
                        if (songsResponse.getError() != null)
                            throw new AmpacheApiException(songsResponse.getError());
                    subscriber.onNext(songsResponse.getSongs());
                    subscriber.onCompleted();
                } catch (Exception e) {
                    subscriber.onError(e);
                }
            }
        })
                .doOnError(doOnError)
                .retry(18)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread());
    }

    /**
     * get a list of all the songs that match the filter
     */
    public Observable<List<Song>> searchSongs(final String filter) {
        return Observable.create(new OnSubscribe<List<Song>>() {

            @Override
            public void call(final Subscriber<? super List<Song>> subscriber) {
                try {
                    SongsResponse songsResponse = getRawRequest().searchSongs(AmpacheSession.INSTANCE.getHandshakeResponse().getAuth(), filter);
                    if (songsResponse.getError()!=null) throw new AmpacheApiException(songsResponse.getError());
                    subscriber.onNext(songsResponse.getSongs());
                    subscriber.onCompleted();
                } catch (Exception e) {
                    subscriber.onError(e);
                }
            }
        })
                .doOnError(doOnError)
                .retry(18)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread());
    }

    /**
     * get a list of songs from given album
     */
    public Observable<List<Song>> getSongsFromAlbum(final String albumId) {
        return Observable.create(new OnSubscribe<List<Song>>() {

            @Override
            public void call(final Subscriber<? super List<Song>> subscriber) {
                try {
                    SongsResponse songssResponse =
                            getRawRequest().getSongsFromAlbum(AmpacheSession.INSTANCE.getHandshakeResponse().getAuth(), albumId);
                    if (songssResponse.getError()!=null) throw new AmpacheApiException(songssResponse.getError());
                    subscriber.onNext(songssResponse.getSongs());
                    subscriber.onCompleted();
                } catch (Exception e) {
                    subscriber.onError(e);
                }
            }
        })
                .doOnError(doOnError)
                .retry(9)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread());
    }

    /**
     * get a list of songs from given album
     */
    public Observable<List<Song>> getSongsFromArtist(final String artistId) {
        return Observable.create(new OnSubscribe<List<Song>>() {

            @Override
            public void call(final Subscriber<? super List<Song>> subscriber) {
                try {
                    SongsResponse songssResponse =
                            getRawRequest().getArtistSongs(AmpacheSession.INSTANCE.getHandshakeResponse().getAuth(), artistId);
                    if (songssResponse.getError()!=null) throw new AmpacheApiException(songssResponse.getError());
                    subscriber.onNext(songssResponse.getSongs());
                    subscriber.onCompleted();
                } catch (Exception e) {
                    subscriber.onError(e);
                }
            }
        })
                .doOnError(doOnError)
                .retry(9)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread());
    }

    /**
     * get a list of songs from given album
     */
    public Observable<List<TagEntity>> getTags() {
        return Observable.create(new OnSubscribe<List<TagEntity>>() {

            @Override
            public void call(final Subscriber<? super List<TagEntity>> subscriber) {
                try {
                    TagsResponse songssResponse =
                            getRawRequest().getTags(AmpacheSession.INSTANCE.getHandshakeResponse().getAuth());
                    if (songssResponse.getError()!=null) throw new AmpacheApiException(songssResponse.getError());
                    subscriber.onNext(songssResponse.getTags());
                    subscriber.onCompleted();
                } catch (Exception e) {
                    subscriber.onError(e);
                }
            }
        })
                .doOnError(doOnError)
                .retry(9)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread());
    }

    /**
     * get a list of songs from given album
     */
    public Observable<List<Playlist>> getPlaylists() {
        return Observable.create(new OnSubscribe<List<Playlist>>() {

            @Override
            public void call(final Subscriber<? super List<Playlist>> subscriber) {
                try {
                    PlaylistsResponse songssResponse =
                            getRawRequest().getPlaylists(AmpacheSession.INSTANCE.getHandshakeResponse().getAuth());
                    if (songssResponse.getError()!=null) throw new AmpacheApiException(songssResponse.getError());
                    subscriber.onNext(songssResponse.getPlaylists());
                    subscriber.onCompleted();
                } catch (Exception e) {
                    subscriber.onError(e);
                }
            }
        })
                .doOnError(doOnError)
                .retry(9)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread());
    }

    /**
     * get a list of songs from given album
     */
    public Observable<List<Song>> getTagSongs(final String tagId) {
        return Observable.create(new OnSubscribe<List<Song>>() {

            @Override
            public void call(final Subscriber<? super List<Song>> subscriber) {
                try {
                    SongsResponse songsResponse =
                            getRawRequest().getTagSongs(AmpacheSession.INSTANCE.getHandshakeResponse().getAuth(), tagId);
                    if (songsResponse.getError()!=null) throw new AmpacheApiException(songsResponse.getError());
                    subscriber.onNext(songsResponse.getSongs());
                    subscriber.onCompleted();
                } catch (Exception e) {
                    subscriber.onError(e);
                }
            }
        })
                .doOnError(doOnError)
                .retry(9)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread());
    }

    public Observable<Song> getSong(final String songId) {
        return Observable.create(new OnSubscribe<Song>() {
            @Override
            public void call(final Subscriber<? super Song> subscriber){
                try {
                    SongsResponse songsResponse =
                            getRawRequest().getSong(AmpacheSession.INSTANCE.getHandshakeResponse().getAuth(), songId);
                    if (songsResponse.getError() != null) throw new AmpacheApiException(songsResponse.getError());
                    Song result = null;
                    if (songsResponse.getSongs()!=null && songsResponse.getSongs().size() > 0) {
                        result = songsResponse.getSongs().get(0);
                    }
                    subscriber.onNext(result);
                    subscriber.onCompleted();
                } catch (Exception e) {
                    subscriber.onError(e);
                }
            }
        })
                .doOnError(doOnError)
                .retry(2)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread());
    }

    /**
     * get a list of songs from given album
     */
    public Observable<List<Playlist>> getPlaylist(final String playlistId) {
        return Observable.create(new OnSubscribe<List<Playlist>>() {

            @Override
            public void call(final Subscriber<? super List<Playlist>> subscriber) {
                try {
                    PlaylistsResponse songssResponse =
                            getRawRequest().getPlaylist(AmpacheSession.INSTANCE.getHandshakeResponse().getAuth(), playlistId);
                    if (songssResponse.getError()!=null) throw new AmpacheApiException(songssResponse.getError());
                    subscriber.onNext(songssResponse.getPlaylists());
                    subscriber.onCompleted();
                } catch (Exception e) {
                    subscriber.onError(e);
                }
            }
        })
                .doOnError(doOnError)
                .retry(9)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread());
    }

    /**
     * get a list of songs from given album
     */
    public Observable<List<Song>> getPlaylistSongs(final String playlistId) {
        return Observable.create(new OnSubscribe<List<Song>>() {

            @Override
            public void call(final Subscriber<? super List<Song>> subscriber) {
                try {
                    SongsResponse songsResponse =
                            getRawRequest().getPlaylistSongs(AmpacheSession.INSTANCE.getHandshakeResponse().getAuth(), playlistId);
                    if (songsResponse.getError()!=null) throw new AmpacheApiException(songsResponse.getError());
                    subscriber.onNext(songsResponse.getSongs());
                    subscriber.onCompleted();
                } catch (Exception e) {
                    subscriber.onError(e);
                }
            }
        })
                .doOnError(doOnError)
                .retry(9)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread());
    }

    /**
     * get a list of songs from given album
     */
    public Observable<List<Song>> createPlaylist(final String name) {
        return Observable.create(new OnSubscribe<List<Song>>() {

            @Override
            public void call(final Subscriber<? super List<Song>> subscriber) {
                try {
                    SongsResponse songsResponse =
                            getRawRequest().createPlaylist(AmpacheSession.INSTANCE.getHandshakeResponse().getAuth(), name, PlaylistType.PUBLIC);
                    if (songsResponse.getError()!=null) throw new AmpacheApiException(songsResponse.getError());
                    subscriber.onNext(songsResponse.getSongs());
                    subscriber.onCompleted();
                } catch (Exception e) {
                    subscriber.onError(e);
                }
            }
        })
                .doOnError(doOnError)
                .retry(9)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread());
    }

    /**
     * ping the server to stay logged in
     */
    public Observable<PingResponse> ping() {
        return Observable.create(new OnSubscribe<PingResponse>() {

            @Override
            public void call(final Subscriber<? super PingResponse> subscriber) {
                try {
                    PingResponse pingResponse = getRawRequest().ping(AmpacheSession.INSTANCE.getHandshakeResponse().getAuth());
                    HandshakeResponse handshakeResponse = AmpacheSession.INSTANCE.getHandshakeResponse();
                    handshakeResponse.setSession_expire(pingResponse.getSession_expire());
                    AmpacheSession.INSTANCE.setHandshakeResponse(handshakeResponse);
                    subscriber.onNext(pingResponse);
                    subscriber.onCompleted();
                } catch (Exception e) {
                    subscriber.onError(e);
                }
            }
        })
                .doOnError(doOnError)
                .retry(22)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread());
    }

    public void resetCredentials() {
        AmpacheSession.INSTANCE.setAmpachePassword(null);
        AmpacheSession.INSTANCE.setAmpacheUrl(null);
        AmpacheSession.INSTANCE.setAmpacheUser(null);
    }

    Action1<Throwable> doOnError = new Action1<Throwable>() {

        @Override
        public void call(final Throwable throwable) {
            try {
                if ((throwable instanceof AmpacheApiException)) {
                    int code = Integer.parseInt(((AmpacheApiException) throwable).getAmpacheError().getCode());
                    if (code >= 400) {
                        getRawRequest().handshake();
                    }
                }
            }catch (Exception e){
                try {
                    getRawRequest().handshake();
                } catch (Exception e1) {
                    e1.printStackTrace();
                }
            }
        }
    };
}
