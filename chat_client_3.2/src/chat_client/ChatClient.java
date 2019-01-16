/*Now the project only supports client sending files and server receiving files. 
 Actually, it has no difficulty to make client receive files and server send files.
*/

/*
There may be an unrepeatable bug in it. When it occurs, server cannot 
receive a complete image and there are many garbled words displayed
in the textarea of server.
*/

package chat_client;

import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.*;

import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JScrollPane;


@SuppressWarnings("serial")
//窗体类
class CFrame extends Frame {
	Socket client = null;
	TextField port = new TextField("6666");
	TextField ip = new TextField("192.168.127.1");// local TCP/IP protocol stack
	Button button_connect = new Button("connect");
	TextArea text_area = new TextArea(25, 60);
	TextField text_field = new TextField(40);
	Button button_say = new Button("say");
	MyListener my_listener = new MyListener();
	ClientThread cfCT;
	String FilePath;
	Syn syn;
	
	CFrame(String title) {
		super(title);
		init();
		syn = new Syn();
		
		button_connect.addActionListener(my_listener);
		button_say.addActionListener(my_listener);

		addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent e) {
				try {
					client.close();
					System.exit(0);
				} catch (Exception ee) {
					System.exit(0);
//					ee.printStackTrace();
				}
				
			}
		});
		//setVisible(true);
	}

	void init() {
		setLocation(500, 10);
		setSize(400, 400);
		setResizable(false);
		setBackground(Color.gray);
		text_area.setBackground(Color.LIGHT_GRAY);

		Panel North = new Panel();
		North.add(new Label("IP"));
		North.add(this.ip);
		North.add(new Label("port"));
		North.add(this.port);
		North.add(this.button_connect);
		this.add(North, BorderLayout.NORTH);

		Panel Center = new Panel();
		Center.add(this.text_area);
		this.add(Center, BorderLayout.CENTER);

		Panel South = new Panel();
		South.add(new Label("say"));
		South.add(this.text_field);
		South.add(this.button_say);
		this.add(South, BorderLayout.SOUTH);

		pack();
	}
	
	

	class MyListener implements ActionListener {
		
		public void actionPerformed(ActionEvent e) {
			String s = e.getActionCommand();
			if ("connect" == s) {// which button has been clicked
				try {
					button_connect.setEnabled(false);
					int i = Integer.parseInt(port.getText());// get port number
					text_area.append("Socket Connect IP :" + ip.getText() + "\nConnect Port :" + port.getText() + "\n");
					client = new Socket(ip.getText(), i);// IP address and port number. wait for accept() run in server
															// program
					text_area.append("connect ...\n");

					cfCT = new ClientThread(); // receive thread
					cfCT.start();
				} catch (Exception ce) {
					ce.printStackTrace();
				}
			} else if ("say" == s) {
				try {
					String str = text_field.getText();
					PrintWriter cc = new PrintWriter(client.getOutputStream());
					if (str != "") {
						if (str.contains("REC_FILE")) {
							text_area.append(str + "\n");
							String dest_file_path = str.replaceAll(" ", "").replaceAll("REC_FILE", "");
							fileOperate(dest_file_path);
						}
						else if (str.contains("FILE")) {
							String str1 = new String(str);
							String file_path = str1.replaceAll(" ", "").replaceAll("FILE", "");
							cc.write(str + "\n");// tell server there is a picture to send
							cc.flush();// In this step, string displays in server.
							FileInputStream fis = new FileInputStream(file_path);
							int size_f = fis.available();
							System.out.println(size_f);
							DataOutputStream dos = new DataOutputStream(client.getOutputStream());
							byte[] buf = new byte[1024];
							int len = 0;
							int i = size_f / 1024 + 1;

							dos.writeInt(i);
							while ((len = fis.read(buf)) != -1) {
								dos.write(buf, 0, len);
								i--;
							}
							System.out.println(i);


							text_area.append("client say:" + str + "\n");// In this step, string is displayed in client.
							fis.close();


						} else {
							cc.write("client say:" + str + "\n");
							cc.flush();// In this step, string displays in server.
							text_area.append("client say:" + str + "\n");// In this step, string displays in client.
						}
					}
					text_field.setText("");// clear client textfield
				} catch (Exception se) {
					se.printStackTrace();
				}
			}
		}
		
		void fileOperate(String dest_file_path) {
			try {

				DataInputStream dis = new DataInputStream(client.getInputStream());

				FileOutputStream fos = new FileOutputStream(dest_file_path);
				byte[] buf = new byte[1024];
				int len = 0;

				int i = dis.readInt();
				System.out.println(i);
				for (; i != 0 && (len = dis.read(buf)) != -1; i--) {
					fos.write(buf, 0, len);
				}
				fos.close();
				text_area.append("File received!\n");
				if (FilePath.contains(".png") || FilePath.contains(".jpg")) {
					showImage(dest_file_path);
				}

				syn.myNotify();
				Thread.sleep(1000);

			} catch (FileNotFoundException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}

		void showImage(String filename) {
			// String filename;
			BufferedImage image;
			JFrame jf;
			// filename="test.png";
			try {
				image = ImageIO.read(new File(filename));
			} catch (Exception e) {
				javax.swing.JOptionPane.showMessageDialog(null, "Fail to load: " + filename);
				image = null;
			}
			jf = new JFrame("");
			JScrollPane scrollPane = new JScrollPane(new JLabel(new ImageIcon(image)));

			jf.getContentPane().add(scrollPane);
			jf.pack();
			jf.setTitle(filename + " " + image.getWidth() + " x " + image.getHeight());
			jf.setVisible(true);
		}

	}

	class ClientThread extends Thread {
		public void run() {
			try {
				BufferedReader br = new BufferedReader(new InputStreamReader(client.getInputStream()));
				String s = br.readLine();
				while (!"Server say:bye".equals(s) && s != null) {
					if (s.contains("FILE")) {
						FilePath = new String(s);
						text_area.append("You have a file to be received!\nPlease enter 'REC_FILE' plus target file path:\n");

						syn.myWait();
						System.out.println("LOOP FINISHED");
						s = br.readLine();
						System.out.println("string s: " + s);
					}else {
					text_area.append(s + "\n");
					s = br.readLine();
					}
				}
			} catch (Exception re) {
				//re.printStackTrace();
				System.out.println("Interrupted!!!");
			}
		}
	}
	class Syn {
		synchronized void myWait() {
			try {
				wait();
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

		synchronized void myNotify() {
			notify();
		}

	}

}

//主类，启动客户端
public class ChatClient {
	public static void main(String[] args) {

		CFrame cf = new CFrame("Client");
		cf.setVisible(true);
	}
}