# CratesPlus

Paper 1.21.11 / Java 21 向けに再構築した、サーバー内ガチャ・クレートプラグインです。

古い Bukkit 時代の設定資産をなるべく維持しつつ、Paper の現行 API、Adventure、PDC を前提にしています。`pom.xml` や外部ホログラムプラグインは不要です。

## ツヨツヨポイント

- **Paper 1.21.11・Java 21** — Gradle Kotlin DSL でビルド。現行の Material / Enchantment API を使う前提です。
- **依存ゼロのネイティブホログラム** — `TextDisplay` を使うため、HolographicDisplays 等を導入する必要がありません。`Billboard.CENTER` により、どの方向から見ても正面を向きます。
- **タイトル判定なしの GUI** — 独自 `InventoryHolder` で識別します。GUI 名の変更や他プラグインとの衝突で誤作動しません。
- **PDC による堅牢なアイテム・ブロック識別** — クレートキーと設置クレートは表示名ではなく `PersistentDataContainer` で判定します。名前や色を変えても壊れません。
- **多言語 + Adventure Component** — クライアント言語に応じて `en_US` / `ja_JP` を自動選択し、`&a` / `&l` と `§` のカラーコードもそのまま使えます。
- **天井（pity）システム** — プレイヤーごと・クレートごとに失敗回数を永続保存。指定回数以内に目玉報酬を確定させられます。
- **安全なレガシー移行** — 旧 `Item Data`・旧エンチャント名・旧キーを、まずレポートで確認してから移行できます。設定変更前には自動バックアップを作成します。
- **外部への設定送信なし** — 旧 `/crate debug` のように config / data を第三者へアップロードする機能は削除済みです。

## 動作環境

| 項目   | 要件            |
|------|---------------|
| サーバー | Paper 1.21.11 |
| Java | 21 以上         |
| 追加依存 | なし            |

## 導入

1. Java 21 で起動している Paper 1.21.11 サーバーを用意します。
2. `build/libs/CratesPlus-5.0.0.jar` をサーバーの `plugins/` に配置します。
3. 一度起動して生成される `plugins/CratesPlus/config.yml` を編集します。
4. 設定を反映するには `/crate reload`、またはサーバーを再起動します。

ローカルでビルドする場合:

```powershell
.\gradlew.bat shadowJar
```

## 天井の設定

クレートごとに `Pity.Limit` を設定し、天井対象の報酬へ `Pity: true` を付けます。

```yml
Crates:
  Premium:
    Type: KeyCrate
    Pity:
      Limit: 90
    Winnings:
      '1':
        Type: ITEM
        Item Type: DIAMOND
        Amount: 8
        Percentage: 99
      '2':
        Type: ITEM
        Item Type: NETHER_STAR
        Amount: 1
        Percentage: 1
        Pity: true
```

この例では、天井対象を引かなかった回数が 89 回に達すると、90 回目は `Pity: true` の報酬から必ず選ばれます。通常抽選で対象報酬を早く引いた場合もカウントは 0 に戻ります。

複数の報酬を `Pity: true` にした場合、天井時はそれらの `Percentage` を重みとして抽選します。`Always: true` の報酬は天井対象にはできません。進捗は `data.yml` のプレイヤーデータに保存され、再起動後も維持されます。

## レガシー環境からの移行

古い設定を使う場合は、適用前に必ず確認してください。

```text
/crate migratelegacy report
/crate migratelegacy apply
/crate migratelegacy keys <オンラインプレイヤー>
```

- `report`: 変更はせず、移行できる項目と手動対応が必要な項目を表示
- `apply`: `config.yml` をバックアップしてから、対応可能な旧設定を移行
- `keys`: 指定プレイヤーのインベントリ内にある旧形式キーを PDC 形式へ更新

古い値付き `Item Data` のように自動変換できない設定は、レポートで警告します。先にバックアップを取ってから確認・修正してください。

## メッセージと言語

メッセージは `plugins/CratesPlus/messages/` にあります。

- `en_US.yml`: 英語
- `ja_JP.yml`: 日本語

`config.yml` の `Locale` は既定言語です。プレイヤーのクライアント言語に一致するファイルがある場合は、そちらを優先します。メッセージやアイテム名では `&aHello &lWorld` のような従来カラーコードを使えます。

## 主なコマンド

| コマンド                                                   | 説明                    |
|--------------------------------------------------------|-----------------------|
| `/crate`                                               | 一般プレイヤーは未受取キーの受取画面を開く |
| `/crate reload`                                        | 設定を再読み込み              |
| `/crate settings`                                      | 管理用設定 GUI を開く         |
| `/crate create <name>`                                 | KeyCrate の雛形を作成       |
| `/crate rename <old> <new>`                            | クレート名を変更              |
| `/crate delete <name>`                                 | クレートを削除               |
| `/crate give <player/all/alloffline> <crate> [amount]` | クレートまたはキーを配布          |
| `/crate migratelegacy …`                               | 旧設定・旧キーの移行            |

管理操作には `cratesplus.admin` が必要です。

## 設定例

詳しいクレート定義は [`src/main/resources/example_config.yml`](src/main/resources/example_config.yml) を参照してください。実運用の設定ファイルは必ずバックアップしてから変更してください。

## ライセンス

このプロジェクトはリポジトリ同梱の [LICENSE](LICENSE) に従います。
