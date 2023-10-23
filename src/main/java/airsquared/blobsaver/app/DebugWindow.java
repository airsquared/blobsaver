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

    private static final Stage debugStage = new Stage();

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
        final PrintStream sysOut = System.out;
        final PrintStream sysErr = System.err;
        debugStage.setOnHiding(_ -> {
            System.setOut(sysOut);
            System.setErr(sysErr);
        });

        final var myPrintStream = new PrintStream(new OutputStream() {
            @Override
            public void write(int b) {
                sysOut.write(b);
                Utils.runSafe(() -> textArea.appendText(Character.toString(b)));
            }
        });
        debugStage.setOnShowing(_ -> {
            System.setOut(myPrintStream);
            System.setErr(myPrintStream);
        });
    }

    static void toggleShowing() {
        if (debugStage.isShowing()) {
            debugStage.hide();
        } else {
            debugStage.show();
        }
    }

}
