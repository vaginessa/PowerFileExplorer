/*
 * Copyright (C) 2014 Arpit Khurana <arpitkh96@gmail.com>, Vishal Nehra <vishalmeham2@gmail.com>
 *
 * This file is part of Amaze File Manager.
 *
 * Amaze File Manager is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.amaze.filemanager.services;

import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.support.v4.provider.DocumentFile;
import android.widget.Toast;

import net.gnu.explorer.R;
import com.amaze.filemanager.database.CryptHandler;
import com.amaze.filemanager.exceptions.RootNotPermittedException;
import com.amaze.filemanager.filesystem.BaseFile;
import com.amaze.filemanager.filesystem.RootHelper;
import com.amaze.filemanager.utils.DataUtils;
import com.amaze.filemanager.utils.OTGUtil;
import com.amaze.filemanager.utils.cloud.CloudUtil;
import com.amaze.filemanager.utils.files.CryptUtil;
import com.amaze.filemanager.utils.files.Futils;
import com.amaze.filemanager.utils.OpenMode;
import com.cloudrail.si.interfaces.CloudStorage;

import java.util.ArrayList;

public class DeleteTask extends AsyncTask<ArrayList<BaseFile>, String, Boolean> {

    private ArrayList<BaseFile> files;
    private Context ctx;
    private boolean rootMode;
    //private ZipViewer zipViewer;
    private DataUtils dataUtils = DataUtils.getInstance();
	private Runnable r;

    public DeleteTask(Context cd, Runnable r) {
        this.ctx = cd;
		this.r = r;
        rootMode = PreferenceManager.getDefaultSharedPreferences(cd).getBoolean("rootmode", false);
    }

//    public DeleteTask(ContentResolver c, Context cd, ZipViewer zipViewer) {
//        this.cd = cd;
//        rootMode = PreferenceManager.getDefaultSharedPreferences(cd).getBoolean("rootmode", false);
//        this.zipViewer = zipViewer;
//    }

    @Override
    protected void onProgressUpdate(String... values) {
        super.onProgressUpdate(values);
        Toast.makeText(ctx, values[0], Toast.LENGTH_SHORT).show();
    }

    protected Boolean doInBackground(ArrayList<BaseFile>... p1) {
        files = p1[0];
        boolean b = true;
        if(files.size()==0)return true;

        if (files.get(0).isOtgFile()) {
            for (BaseFile a : files) {

                DocumentFile documentFile = OTGUtil.getDocumentFile(a.getPath(), ctx, false);
                 b = documentFile.delete();
            }
        } else if (files.get(0).isDropBoxFile()) {
            CloudStorage cloudStorageDropbox = dataUtils.getAccount(OpenMode.DROPBOX);
            for (BaseFile baseFile : files) {
                try {
                    cloudStorageDropbox.delete(CloudUtil.stripPath(OpenMode.DROPBOX, baseFile.getPath()));
                } catch (Exception e) {
                    e.printStackTrace();
                    b = false;
                    break;
                }
            }
        } else if (files.get(0).isBoxFile()) {
            CloudStorage cloudStorageBox = dataUtils.getAccount(OpenMode.BOX);
            for (BaseFile baseFile : files) {
                try {
                    cloudStorageBox.delete(CloudUtil.stripPath(OpenMode.BOX, baseFile.getPath()));
                } catch (Exception e) {
                    e.printStackTrace();
                    b = false;
                    break;
                }
            }
        } else if (files.get(0).isGoogleDriveFile()) {
            CloudStorage cloudStorageGdrive = dataUtils.getAccount(OpenMode.GDRIVE);
            for (BaseFile baseFile : files) {
                try {
                    cloudStorageGdrive.delete(CloudUtil.stripPath(OpenMode.GDRIVE, baseFile.getPath()));
                } catch (Exception e) {
                    e.printStackTrace();
                    b = false;
                    break;
                }
            }
        } else if (files.get(0).isOneDriveFile()) {
            CloudStorage cloudStorageOnedrive = dataUtils.getAccount(OpenMode.ONEDRIVE);
            for (BaseFile baseFile : files) {
                try {
                    cloudStorageOnedrive.delete(CloudUtil.stripPath(OpenMode.ONEDRIVE, baseFile.getPath()));
                } catch (Exception e) {
                    e.printStackTrace();
                    b = false;
                    break;
                }
            }
        } else {

            for(BaseFile a : files)
                try {
                    (a).delete(ctx, rootMode);
                } catch (RootNotPermittedException e) {
                    e.printStackTrace();
                    b = false;
                }
        }

        // delete file from media database
        if(!files.get(0).isSmb()) {
            try {
                for (BaseFile f : files) {
                    delete(ctx,f.getPath());
                }
            } catch (Exception e) {
                for (BaseFile f : files) {
                    Futils.scanFile(f.getPath(), ctx);
                }
            }
        }

        // delete file entry from encrypted database
        for (BaseFile file : files) {
            if (file.getName().endsWith(CryptUtil.CRYPT_EXTENSION)) {
                CryptHandler handler = new CryptHandler(ctx);
                handler.clear(file.getPath());
            }
        }

        return b;
    }

    @Override
    public void onPostExecute(Boolean b) {
        Intent intent = new Intent("loadlist");
        intent.putExtra("targetPath", files.get(0).getParent());
		ctx.sendBroadcast(intent);

        if (!b) {
            Toast.makeText(ctx, ctx.getResources().getString(R.string.error), Toast.LENGTH_SHORT).show();
//        } else if (zipViewer==null) {
//            Toast.makeText(cd, cd.getResources().getString(R.string.done), Toast.LENGTH_SHORT).show();
        }
		if (r != null) {
			r.run();
		}

//        if (zipViewer!=null) {
//            zipViewer.files.clear();
//        }
    }

    private void delete(final Context context, final String file) {
        final String where = MediaStore.MediaColumns.DATA + "=?";
        final String[] selectionArgs = new String[] {
                file
        };
        final ContentResolver contentResolver = context.getContentResolver();
        final Uri filesUri = MediaStore.Files.getContentUri("external");
        // Delete the entry from the media database. This will actually delete media files.
        contentResolver.delete(filesUri, where, selectionArgs);

    }
}



