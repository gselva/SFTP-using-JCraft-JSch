/*
 * This file is licensed under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.nidhinova.tools.ssh;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;

import com.jcraft.jsch.Channel;
import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.SftpException;

/**
* A SFTP client program that provides easy methods to 
* -file put and delete
* -creation of remote folders
* -deep creation of remote folders (such as /parent/child1/child1.1 ) 
*/
public class SFTPClient {
	private static final Logger _logger = Logger.getLogger(SFTPClient.class);

	private String username;
	private byte[] passphrase;
	private byte[] privatekey = null;
	private JSchUserInfo userinfo = null;
	private String hostname;
	private Session session = null;
	private ChannelSftp sftp = null;

	public SFTPClient(String hostname, String username, String password,
			String passphrase, File privateKeyfile) throws IOException {
		this.hostname = hostname;
		this.username = username;
		this.passphrase = this.str2byte(passphrase);
		this.privatekey = FileUtils.readFileToByteArray(privateKeyfile);
		this.userinfo = new JSchUserInfo(password, passphrase);

	}

	public static void main(String[] args) {
		SFTPClient client = null;
		try {
			client = new SFTPClient(
					"my.secureserver.com",
					"foo",
					"bar",
					"",
					new File(
							"./.ssh/keyfile"));
			client.connect();

			client.deleteRemoteFolder("/parentfoldername", "relativepath/delete/thisfolder");

			client.disconnect();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (JSchException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (SftpException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} finally {
			try {
				if (client != null)
					client.disconnect();
			} catch (Exception e) {

			}
		}

	}

	public void connect() throws JSchException, SftpException {
		JSch jSch = new JSch();
		jSch.addIdentity(this.username, // String userName
				this.privatekey, // byte[] privateKey
				null, // byte[] publicKey
				this.passphrase // byte[] passPhrase
				);

		this.session = jSch.getSession(this.username, this.hostname, 22);
		this.session.setUserInfo(this.userinfo);
		java.util.Properties config = new java.util.Properties();
		config.put("StrictHostKeyChecking", "no");
		session.setConfig(config);

		this.session.connect();
		Channel channel = session.openChannel("sftp");
		this.sftp = (ChannelSftp) channel;
		this.sftp.connect();
		_logger.info("Connected to Server via SFTP...");
	}

	public void disconnect() {
		if (this.sftp != null)
			this.sftp.disconnect();
		if (this.session != null)
			this.session.disconnect();
		_logger.info("Disconnected from Server.");
	}

	public void createRemoteFolder(String root, String relativepath)
			throws SftpException {
		this.sftp.cd(this.sftp.getHome());
		this.sftp.cd(root);
		this.sftp.mkdir(relativepath);
	}

	public void deleteRemoteFolder(String root, String relativepath)
			throws SftpException {
		this.sftp.cd(this.sftp.getHome());
		this.sftp.cd(root+"/"+relativepath);
		_logger.debug(">cd "+ root+"/"+relativepath);
		String pwd = this.sftp.pwd();
		_logger.debug("pwd="+pwd);
		//delete all files
		Vector<ChannelSftp.LsEntry> files;		
		files = this.sftp.ls("*.*");
		_logger.debug("# files = " + (files==null?"0":files.size()));
		
		if(files!=null) {		
		for (int i=0; i<files.size(); i++) {
			ChannelSftp.LsEntry file = files.get(i);
			this.sftp.rm(file.getFilename());
			_logger.debug("Deleted file " + file.getFilename());
		}
		
		}
		
		//delete the folder		
		String foldername = relativepath.substring(relativepath.lastIndexOf('/')+1);
		this.sftp.cd("..");
		pwd = this.sftp.pwd();
		this.sftp.rmdir(foldername);
	}

	public void putFile(String file, String target) throws SftpException {
		_logger.debug("Copying [" + file + "] to [" + target + "]");
		this.sftp.put(file, target);
	}

	public void deleteFile(String file, String target) throws SftpException {
		_logger.debug("Copying [" + file + "] to [" + target + "]");
		this.sftp.put(file, target);
	}

	public boolean checkExists(String folder) {
		try {
			this.sftp.ls(folder);
			return true;
		} catch (SftpException e) {
			return false;
		}
	}

	public void deepCreateRemoteFolder(String root, String relativepath)
			throws SftpException {
		this.sftp.cd(this.sftp.getHome());
		this.sftp.cd(root);
		List<File> folders = new ArrayList<File>();
		folders.add(new File(relativepath));
		try {
			File path = new File(relativepath);
			while (path != null) {
				path = new File(path.getParent());
				if (path != null) {
					folders.add(path);
				}
			}
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		String path = null;
		for (int i = folders.size() - 1; i > -1; i--) {
			path = folders.get(i).getPath().replace('\\', '/');
			this.sftp.mkdir(path);
			_logger.debug("Created folder >>" + path);
		}

	}

	private byte[] str2byte(String str, String encoding) {
		if (str == null)
			return null;
		try {
			return str.getBytes(encoding);
		} catch (java.io.UnsupportedEncodingException e) {
			return str.getBytes();
		}
	}

	private byte[] str2byte(String str) {
		return str2byte(str, "UTF-8");
	}

}
