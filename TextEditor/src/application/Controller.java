package application;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.Scanner;

import javafx.application.Platform;
import javafx.beans.value.WritableDoubleValue;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Bounds;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.CheckMenuItem;
import javafx.scene.control.Control;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.RadioMenuItem;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TabPane.TabClosingPolicy;
import javafx.scene.control.TextArea;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;

public class Controller implements Initializable {
	// short cuts
	public static final KeyCombination saveShortcut = new KeyCodeCombination(KeyCode.S, KeyCombination.CONTROL_DOWN);
	public static final KeyCombination openShortcut = new KeyCodeCombination(KeyCode.O, KeyCombination.CONTROL_DOWN);

	// anchor pane as base level of UI
	@FXML
	public AnchorPane ap;

	// the file being worked with
	public File file;

	// the path of the file being worked with
	@FXML
	public String filePath;

	// text area for user input
	@FXML
	public TextArea userText;
	public Text helper = new Text();

	// line counter
	@FXML
	public TextArea counterColumn;
	
	@FXML
	public CheckMenuItem lineCounter;
	
	@FXML
	public CheckMenuItem themeDefault;
	public CheckMenuItem themeDark;
	public CheckMenuItem themeOrange;
	
	@FXML
	public TabPane tabPane;
	//public Tab tab;
	
	public Object pauseLock = new Object();
	public boolean exit = false;
	
	public File userWorkspace;
	
	public Button closeButton;

	// Created a constantly running thread to update the counterColumn.
	Thread update = new Thread(new Runnable() {
		@Override
		public void run() {
			Runnable updater = new Runnable() {
				@Override
				public void run() {
					drawCounter();
				}
			};

			while (true) {
				try {
					Thread.sleep(5);
				} catch (InterruptedException ex) {
				}
				Platform.runLater(updater);
			}
		}
	});

	@Override
	public void initialize(URL arg0, ResourceBundle arg1) {
		update.setDaemon(true);
		update.start();
		userText.scrollTopProperty().bindBidirectional(counterColumn.scrollTopProperty());
		lineCounter.setSelected(true);
		counterColumn.appendText(String.valueOf(1));
		themeDefault.setSelected(true);
		tabPane.setTabClosingPolicy(TabClosingPolicy.SELECTED_TAB);
		
		if(new File("src/resources/workspace.txt").exists()) {
			userWorkspace = new File(initWorkspace());
		}

//		if(!new File("src/resources/workspace.txt").exists()) {
//			Alert setWorkspace = new Alert(AlertType.CONFIRMATION);
//			setWorkspace.setTitle("Setup Workspace");
//			setWorkspace.setHeaderText("Do you wish to set your workspace?");
//			setWorkspace.setContentText("We've detected you haven't set up your workspace yet, do you wish to set one up now?");
//			Optional<ButtonType> result = setWorkspace.showAndWait();
//			if(result.get() == ButtonType.OK) {
//				setWorkspace();
//				System.out.println("Workspace set to" + userWorkspace.getAbsolutePath());
//			}else {
//				
//			}
//		}
	}

	// Update continuously.
	public void update() {
	}

	// All key listeners when pressed for short cuts.
	@FXML
	public void keyListenerPressed(KeyEvent event) {
		if (saveShortcut.match(event)) {
			saveFile();
		} else if (openShortcut.match(event)) {
			openFile();
		}
	}

	// Clears the work space for a brand new file.
	@FXML
	public void newFile() {
		userText.clear();
		file = null;
	}

	// Save file, where if the file is equal to null then the program will open the
	// file chooser for the user to select the directory where they want the file to
	// be saved. Otherwise the file will just be saved to the current file.
	@FXML
	public void saveFile() {
		if (file == null) {
			FileChooser saveFileChooser = new FileChooser();
			saveFileChooser.setInitialDirectory(userWorkspace);
			saveFileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Text doc(*.txt)", "*.txt"));
			file = saveFileChooser.showSaveDialog(ap.getScene().getWindow());
		}

		if (file.getName().endsWith(".txt")) {
			try {
				filePath = file.getAbsolutePath();
				Stage primStage = (Stage) ap.getScene().getWindow();
				primStage.setTitle(filePath);
				Tab tab = tabPane.getSelectionModel().getSelectedItem();
				tab.setId(filePath);
				tab.setText(file.getName());

				BufferedWriter writer = new BufferedWriter(new FileWriter(file));
				PrintWriter output = new PrintWriter(writer);
				output.write(userText.getText());
				output.flush();
				output.close();
			} catch (IOException e) {
				System.out.println(file.getName() + " has no valid file-extenstion.");
				e.printStackTrace();
			}
		}

	}

	// Open text file, when the file is equal to null, open a file chooser that
	// allows the user to select what file they want to open.
	// If a file already exists within workspace, use a scanner to read the text and
	// append it to the text area for future editing. The
	// file can then be edited 'quick saved' without having to open another file
	// chooser.
	@FXML
	public void openFile() {
		FileChooser openFileChooser = new FileChooser();
		openFileChooser.setInitialDirectory(userWorkspace);
		openFileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Text doc(*.txt)", "*.txt"));
		file = openFileChooser.showOpenDialog(ap.getScene().getWindow());

		userText.clear();

		if (file != null) {
			if (file.getName().endsWith(".txt")) {
				filePath = file.getAbsolutePath();
				Stage primStage = (Stage) ap.getScene().getWindow();
				primStage.setTitle(filePath);
				Tab tab = tabPane.getSelectionModel().getSelectedItem();
				tab.setId(filePath);
				tab.setText(file.getName());

				writeToUserText();
			}
		}
	}

	// Constantly updating the number of lines in the userText text area. Loops
	// through all paragraphs in the userText text area until it finds one that is
	// wrapped.
	// Once wrapped text is found, I use bounds to check the dimensions of the text
	// then reset and restore the wrapping width on the helper and recording the
	// change.
	// If the paragraph size is less than the current wrappingWidth then increment;
	// Otherwise use the current paragaph with "wrappingWidth" set to the actual
	// wrappingWidth of the textArea text.
	public int getCounter() {
		int currentRowCount = 0;

		if (this.userText.isWrapText()) {
			Text text = (Text) userText.lookup(".text");
			if (text == null) {
				return currentRowCount;
			}
			helper.setFont(userText.getFont());
			for (CharSequence paragraph : userText.getParagraphs()) {
				helper.setText(paragraph.toString());
				Bounds localBounds = helper.getBoundsInLocal();

				double paragraphWidth = localBounds.getWidth();
				if (paragraphWidth > text.getWrappingWidth()) {
					double oldHeight = localBounds.getHeight();
					helper.setWrappingWidth(text.getWrappingWidth());
					double newHeight = helper.getBoundsInLocal().getHeight();
					helper.setWrappingWidth(0.0D);

					int paragraphLineCount = Double.valueOf(newHeight / oldHeight).intValue();
					currentRowCount += paragraphLineCount;
				} else {
					currentRowCount += 1;
				}
			}

		} else {
			currentRowCount = userText.getParagraphs().size();
		}
		return currentRowCount;

	}

	// Draws our counter to the screen.
	public void drawCounter() {
		counterColumn.clear();
		int currentRowCount = getCounter();
		for (int i = 1; i <= currentRowCount; i++) {
			counterColumn.appendText(String.valueOf(i) + "\n");
		}
	}
	
	// WIP toggle for the line counter. Not added yet.
	@FXML
	public void lineCounterToggle(){
		if(lineCounter.isSelected() == false) {
			try {
				synchronized(update) {
					update.wait();

				}
			} catch (InterruptedException e) {
				System.out.println("InterruptedException");
				e.printStackTrace();
			}
			counterColumn.clear();
			counterColumn.setVisible(false);
			userText.setPrefWidth(800);
			counterColumn.setPrefWidth(0);
		}else {
			synchronized(update) {
				update.notify();
			}
			counterColumn.setVisible(true);
			counterColumn.setPrefWidth(24);
			userText.setPrefWidth(776);
		}
	}
	
	@FXML
	public void defaultSelected() {
		ArrayList<CheckMenuItem> themes = initThemes();
		
		for(int i = 0; i < themes.size(); i++) {
			themes.get(i).setSelected(false);
		}
		themeDefault.setSelected(true);
		ap.getStylesheets().clear();
		ap.getStylesheets().add(getClass().getResource("application.css").toExternalForm());
	}
	
	@FXML
	public void darkSelected() {
		ArrayList<CheckMenuItem> themes = initThemes();
		
		for(int i = 0; i < themes.size(); i++) {
			themes.get(i).setSelected(false);
		}
		themeDark.setSelected(true);
		ap.getStylesheets().clear();
		ap.getStylesheets().add(getClass().getResource("darkmode.css").toExternalForm());
	}
	
	public ArrayList<CheckMenuItem> initThemes() {
		ArrayList<CheckMenuItem> themes = new ArrayList<CheckMenuItem>();
		themes.add(themeDark);
		themes.add(themeDefault);
		themes.add(themeOrange);
		
		return themes;
	}
	
	@FXML
	public void setWorkspace() {
		DirectoryChooser workspaceChooser = new DirectoryChooser();
		File selectedWorkspace = workspaceChooser.showDialog(ap.getScene().getWindow());
		String workspacePath = selectedWorkspace.getAbsolutePath();
		
		try {
			BufferedWriter writer = new BufferedWriter(new FileWriter(new File("D:\\git\\TextEditor\\TextEditor\\src\\resources\\workspace.txt")));
			PrintWriter output = new PrintWriter(writer);
			output.write(workspacePath);
			output.flush();
			output.close();
		} catch (IOException e) {
			System.out.println("Error");
			e.printStackTrace();
		}
		
		userWorkspace = new File(workspacePath);
	}
	
	public String initWorkspace() {

		File workspaceHolder = new File("src/resources/workspace.txt");
		String workspacePath = null;
		try {
			Scanner reader = new Scanner(workspaceHolder);
			while(reader.hasNextLine()) {
				workspacePath = reader.nextLine();
			}
			reader.close();
			return workspacePath;
		} catch (FileNotFoundException e) {
			System.out.println("workspace path not set");
			e.printStackTrace();
		}
		System.out.println("workspace path not set");
		return workspacePath;
	}
	
	@FXML
	public void createTab() {
		Tab newTab = new Tab("Untitled");
		EventHandler<Event> tabSelected = new EventHandler<Event>() {
			@Override
			public void handle(Event event) {
				tabSelected();
			}
		};
		newTab.setOnSelectionChanged(tabSelected);
		
		tabPane.getTabs().add(tabPane.getTabs().size() -1, newTab);
		tabPane.getSelectionModel().select(tabPane.getTabs().size() -2);
	}
	
	@FXML
	public void tabSelected() {
		if(!tabPane.getSelectionModel().getSelectedItem().getText().equals("Untitled")) {
			file = new File(tabPane.getSelectionModel().getSelectedItem().getId());
			Stage primStage = (Stage) ap.getScene().getWindow();
			primStage.setTitle(filePath);
			userText.clear();
			writeToUserText();
		}else {
			userText.clear();
		}
	}
	
	public void writeToUserText() {
		try {
			Scanner reader = new Scanner(file);
			while (reader.hasNextLine()) {
				String current = reader.nextLine();
				userText.appendText(current + "\n");
			}
			reader.close();
		} catch (FileNotFoundException e) {
			System.out.println("Could not read file: " + file.getName());
			e.printStackTrace();
		}
	}
	

}