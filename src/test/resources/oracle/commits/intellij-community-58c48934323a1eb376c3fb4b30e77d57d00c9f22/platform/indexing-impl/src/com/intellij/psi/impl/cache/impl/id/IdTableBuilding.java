// Copyright 2000-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package com.intellij.psi.impl.cache.impl.id;

import com.intellij.ide.highlighter.custom.CustomFileTypeLexer;
import com.intellij.ide.highlighter.custom.SyntaxTable;
import com.intellij.lang.Language;
import com.intellij.lang.cacheBuilder.CacheBuilderRegistry;
import com.intellij.lang.cacheBuilder.DefaultWordsScanner;
import com.intellij.lang.cacheBuilder.SimpleWordsScanner;
import com.intellij.lang.cacheBuilder.WordsScanner;
import com.intellij.lang.findUsages.LanguageFindUsages;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.fileTypes.LanguageFileType;
import com.intellij.openapi.fileTypes.PlainTextFileType;
import com.intellij.openapi.fileTypes.impl.CustomSyntaxTableFileType;
import com.intellij.psi.CustomHighlighterTokenType;
import com.intellij.psi.tree.TokenSet;
import com.intellij.util.indexing.FileBasedIndex;
import com.intellij.util.text.CharArrayUtil;
import com.intellij.util.text.CharSequenceSubSequence;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.IntPredicate;

public final class IdTableBuilding {
  private IdTableBuilding() {
  }

  public interface ScanWordProcessor {
    void run(CharSequence chars, char @Nullable [] charsArray, int start, int end);
  }

  public static @Nullable IdIndexer getFileTypeIndexer(FileType fileType) {
    final IdIndexer extIndexer = getIndexer(fileType);
    if (extIndexer != null) {
      return extIndexer;
    }

    final WordsScanner customWordsScanner = CacheBuilderRegistry.getInstance().getCacheBuilder(fileType);
    if (customWordsScanner != null) {
      return createDefaultIndexer(customWordsScanner);
    }

    if (fileType instanceof LanguageFileType) {
      final Language lang = ((LanguageFileType)fileType).getLanguage();
      WordsScanner scanner = LanguageFindUsages.getWordsScanner(lang);
      if (scanner == null) {
        scanner = new SimpleWordsScanner();
      }
      return createDefaultIndexer(scanner);
    }

    if (fileType instanceof CustomSyntaxTableFileType) {
      return new ScanningIdIndexer() {
        @Override
        protected WordsScanner createScanner() {
          return createCustomFileTypeScanner(((CustomSyntaxTableFileType)fileType).getSyntaxTable());
        }
      };
    }

    return null;
  }

  private static IdIndexer getIndexer(@NotNull FileType fileType) {
    if (fileType == PlainTextFileType.INSTANCE && FileBasedIndex.IGNORE_PLAIN_TEXT_FILES) return null;
    return IdIndexers.INSTANCE.forFileType(fileType);
  }

  @Contract(value = "_ -> new", pure = true)
  public static @NotNull IdIndexer createDefaultIndexer(@NotNull WordsScanner scanner) {
    return new ScanningIdIndexer() {
      @Override
      protected WordsScanner createScanner() {
        return scanner;
      }
    };
  }

  @Contract("_ -> new")
  public static @NotNull WordsScanner createCustomFileTypeScanner(final @NotNull SyntaxTable syntaxTable) {
    return new DefaultWordsScanner(new CustomFileTypeLexer(syntaxTable, true),
                                   TokenSet.create(CustomHighlighterTokenType.IDENTIFIER),
                                   TokenSet.create(CustomHighlighterTokenType.LINE_COMMENT,
                                                   CustomHighlighterTokenType.MULTI_LINE_COMMENT),
                                   TokenSet.create(CustomHighlighterTokenType.STRING, CustomHighlighterTokenType.SINGLE_QUOTED_STRING));

  }

  public static void scanWords(final ScanWordProcessor processor, final CharSequence chars, final int startOffset, final int endOffset) {
    scanWords(processor, chars, CharArrayUtil.fromSequenceWithoutCopying(chars), startOffset, endOffset, false);
  }

  public static void scanWords(final ScanWordProcessor processor,
                               final CharSequence chars,
                               final char @Nullable [] charArray,
                               final int startOffset,
                               final int endOffset,
                               final boolean mayHaveEscapes) {
    scanWords(processor, chars, charArray, startOffset, endOffset, mayHaveEscapes, IdTableBuilding::isWordCodePoint);
  }

  public static boolean isWordCodePoint(int codePoint) {
    return (codePoint >= 'a' && codePoint <= 'z') ||
           (codePoint >= 'A' && codePoint <= 'Z') ||
           (codePoint >= '0' && codePoint <= '9') ||
           (Character.isJavaIdentifierStart(codePoint) && codePoint != '$');
  }

  @SuppressWarnings("DuplicatedCode")
  public static void scanWords(final ScanWordProcessor processor,
                               CharSequence chars,
                               final char @Nullable [] charArray,
                               final int startOffset,
                               final int endOffset,
                               final boolean mayHaveEscapes,
                               final IntPredicate isWordCodePoint) {
    int index = startOffset;
    boolean hasArray = charArray != null;
    if (!hasArray) {
      // Workaround lack of overload to specify endOffset for Character.codePointAt
      // with CharSequence as a parameter
      chars = new CharSequenceSubSequence(chars, 0, endOffset);
    }
    ScanWordsLoop:
    while (true) {
      int startIndex = index;
      while (true) {
        if (index >= endOffset) break ScanWordsLoop;
        int codePoint = hasArray ? Character.codePointAt(charArray, index, endOffset)
                                 : Character.codePointAt(chars, index);
        index += Character.charCount(codePoint);
        if (isWordCodePoint.test(codePoint)) {
          break;
        }
        if (mayHaveEscapes && codePoint == '\\') index++; //the next symbol is for escaping
        startIndex = index;
      }
      int endIndex = index;
      while (true) {
        if (index >= endOffset) break;
        int codePoint = hasArray ? Character.codePointAt(charArray, index, endOffset)
                                 : Character.codePointAt(chars, index);
        index += Character.charCount(codePoint);
        if (!isWordCodePoint.test(codePoint)) {
          break;
        }
        endIndex = index;
      }
      if (endIndex - startIndex > 100) continue; // Strange limit but we should have some!

      processor.run(chars, charArray, startIndex, endIndex);
    }
  }

}
