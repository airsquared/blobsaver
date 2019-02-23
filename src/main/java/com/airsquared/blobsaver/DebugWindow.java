/*
 * Copyright (c) 2019  airsquared
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

package com.airsquared.blobsaver;

import de.codecentric.centerdevice.util.StageUtils;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.control.TextArea;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.io.OutputStream;
import java.io.PrintStream;

class DebugWindow {

    private static final PrintStream sysOut = System.out;

    private static Stage debugStage = new Stage();
    private static PrintStream myPrintStream;

    private static boolean mWasDirectlyClosed = true;
    private static boolean retainFocus = true;

    static {
        VBox vBox = new VBox();
        vBox.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
        TextArea textArea = new TextArea();
        VBox.setVgrow(textArea, Priority.ALWAYS);
        textArea.setEditable(false);
        textArea.setWrapText(true);
        textArea.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
        vBox.getChildren().add(textArea);
        debugStage.setTitle("Debug Log");
        debugStage.setScene(new Scene(vBox));
        debugStage.setOnCloseRequest(event -> {
            directlyClose();
            event.consume();
        });
        debugStage.setOnShown((e) -> {
            debugStage.setX(debugStage.getX());
            debugStage.setY(debugStage.getY());
        });

        myPrintStream = new PrintStream(new OutputStream() {
            void appendText(String valueOf) {
                if (Platform.isFxApplicationThread()) {
                    textArea.appendText(valueOf);
                } else {
                    Platform.runLater(() -> textArea.appendText(valueOf));
                }
            }

            @Override
            public void write(int b) {
                sysOut.write(b);
                appendText(String.valueOf((char) b));
            }
        });
    }

    static void show() {
        debugStage.show();
        if (retainFocus) {
            getFocus();
        }
        System.setOut(myPrintStream);
        System.setErr(myPrintStream);
    }

    // this will make it reopen when the primaryStage is re-shown
    // supposed to be called when something else made the DebugWindow close,
    // so the user expects it to be reopened when the primaryStage is opened again
    static void indirectlyClose() {
        hide(false);
    }

    static void directlyClose() {
        hide(true);
    }

    // this should not be called directly, but rather indirectly from directlyClose() or indirectlyClose()
    // (pun not intended) ;)
    private static void hide(boolean wasDirectlyClosed) {
        setRetainFocus();
        setWasDirectlyClosed(wasDirectlyClosed);
        debugStage.hide();
        System.setOut(sysOut);
        System.setErr(sysOut);
    }

    private static void setRetainFocus() {
        retainFocus = StageUtils.getFocusedStage().isPresent() &&
            StageUtils.getFocusedStage().get().equals(getDebugStage());
    }

    static boolean willRetainFocus() {
        return retainFocus;
    }

    static boolean wasDirectlyClosed() {
        return mWasDirectlyClosed;
    }

    static boolean wasIndirectlyClosed() {
        return !mWasDirectlyClosed;
    }

    private static void setWasDirectlyClosed(boolean wasDirectlyClosed) {
        mWasDirectlyClosed = wasDirectlyClosed;
    }


    static Stage getDebugStage() {
        return debugStage;
    }

    static void getFocus() {
        //only get focus when about stage doesn't exist, since about stage is always on top
        if (Main.getStage("About") == null) {
            debugStage.requestFocus();
            debugStage.toFront();
        }
    }

    static boolean isShowing() {
        return debugStage.isShowing();
    }
}
