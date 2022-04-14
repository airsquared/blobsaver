/*
 * Copyright (c) 2021  airsquared
 *
 * This file is part of blobsaver.
 *
 * blobsaver is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, version 3 of the License.
 *
 * blobsaver is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with blobsaver.  If not, see <https://www.gnu.org/licenses/>.
 */

package airsquared.blobsaver.app;

import javafx.scene.Scene;
import javafx.scene.control.TextArea;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.io.OutputStream;
import java.io.PrintStream;

class DebugWindow {

    private static final PrintStream sysOut = System.out;
    private static final PrintStream sysErr = System.err;
    private static final Stage debugStage = new Stage();
    private static final PrintStream myPrintStream;

    static {
        var vBox = new VBox();
        vBox.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
        var textArea = new TextArea();
        VBox.setVgrow(textArea, Priority.ALWAYS);
        textArea.setEditable(false);
        textArea.setWrapText(true);
        textArea.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
        vBox.getChildren().add(textArea);
        debugStage.setTitle("Debug Log");
        debugStage.setScene(new Scene(vBox));
        debugStage.setOnCloseRequest((event) -> {
            hide();
            event.consume();
        });

        myPrintStream = new PrintStream(new OutputStream() {
            @Override
            public void write(int b) {
                sysOut.write(b);
                Utils.runSafe(() -> textArea.appendText(String.valueOf((char) b)));
            }
        });
    }

    static void show() {
        debugStage.show();
        System.setOut(myPrintStream);
        System.setErr(myPrintStream);
    }

    static void hide() {
        debugStage.hide();
        System.setOut(sysOut);
        System.setErr(sysErr);
    }

    static boolean isShowing() {
        return debugStage.isShowing();
    }

    static void toggleShowing() {
        if (isShowing()) {
            hide();
        } else {
            show();
        }
    }

}
