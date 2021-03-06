// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.util.ui;

import com.intellij.openapi.util.Getter;
import com.intellij.ui.PopupMenuListenerAdapter;
import com.intellij.ui.TextFieldWithHistory;
import com.intellij.ui.TextFieldWithHistoryWithBrowseButton;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.containers.Convertor;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.event.PopupMenuEvent;
import java.awt.event.ActionEvent;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.List;
import java.util.function.BiFunction;

/**
 * @author Irina.Chernushina on 1/5/2016.
 */
public interface ReadonlyFieldWithHistoryWithBrowseButton {
  JComponent getComponent();
  void set(@NotNull String text);
  @NotNull String get();
  void addListener(Runnable listener);
  void setPreferredWidthToFitText();

  class Builder {
    private BiFunction<ActionEvent, String, String> myActionListener;
    private Getter<List<String>> myHistoryProvider;
    private Convertor<TextFieldWithHistory, ListCellRenderer> myRendererCreator;

    public Builder withRenderer(@NotNull final Convertor<TextFieldWithHistory, ListCellRenderer> convertor) {
      myRendererCreator = convertor;
      return this;
    }

    public Builder withHistoryProvider(@NotNull final Getter<List<String>> provider) {
      myHistoryProvider = provider;
      return this;
    }

    public Builder withActionListener(@NotNull final BiFunction<ActionEvent, String, String> listener) {
      myActionListener = listener;
      return this;
    }

    public ReadonlyFieldWithHistoryWithBrowseButton build() {
      final TextFieldWithHistoryWithBrowseButton field = new TextFieldWithHistoryWithBrowseButton();
      final TextFieldWithHistory textFieldWithHistory = field.getChildComponent();
      textFieldWithHistory.setHistorySize(-1);
      textFieldWithHistory.setMinimumAndPreferredWidth(0);
      textFieldWithHistory.setEditable(false);

      final ReadonlyFieldWithHistoryWithBrowseButton wrapper = createReadonlyFieldWrapper(field);

      if (myActionListener != null) {
        field.getButton().addActionListener(e -> {
          final String value = myActionListener.apply(e, wrapper.get());
          if (value != null) {
            wrapper.set(value);
          }
        });
      }
      if (myHistoryProvider != null) {
        textFieldWithHistory.addPopupMenuListener(new PopupMenuListenerAdapter() {
          @Override
          public void popupMenuWillBecomeVisible(PopupMenuEvent e) {
            SwingHelper.setHistory(textFieldWithHistory, ContainerUtil.notNullize(myHistoryProvider.get()), false);
          }
        });
      }
      if (myRendererCreator != null) {
        final ListCellRenderer renderer = myRendererCreator.convert(textFieldWithHistory);
        if (renderer != null) {
          //noinspection GtkPreferredJComboBoxRenderer
          textFieldWithHistory.setRenderer(renderer);
        }
      }
      return wrapper;
    }

    @NotNull
    public static ReadonlyFieldWithHistoryWithBrowseButton createReadonlyFieldWrapper(final TextFieldWithHistoryWithBrowseButton field) {
      return new ReadonlyFieldWithHistoryWithBrowseButton() {
        @Override
        public JComponent getComponent() {
          return field;
        }

        @Override
        public void set(@NotNull String text) {
          final TextFieldWithHistory component = field.getChildComponent();
          if (!component.getHistory().contains(text)) {
            component.setTextAndAddToHistory(text);
          }
          component.setSelectedItem(text);
        }

        @NotNull
        @Override
        public String get() {
          final Object item = field.getChildComponent().getSelectedItem();
          return item == null ? "" : item.toString().trim();
        }

        @Override
        public void addListener(final Runnable listener) {
          field.getChildComponent().addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent e) {
              listener.run();
            }
          });
        }

        @Override
        public void setPreferredWidthToFitText() {
          SwingHelper.setPreferredWidthToFitText(field);
        }
      };
    }
  }
}
