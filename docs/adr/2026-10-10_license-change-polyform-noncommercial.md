---
date: 2026-10-10
category: adr
---

# 2026-10-10 ライセンスを PolyForm Noncommercial 1.0.0 へ切替

独自の LittleMaid Licence から、**PolyForm Noncommercial License 1.0.0** に切り替えた。AGPLv3 への変更も検討したので、その却下理由も記録する。

## 背景

旧 LittleMaid Licence は「非商用のみ・改変/リミックス/再配布/動画利用可」を定めていたが、Notice 伝達義務も違反時の失効条項も特許条項も持たず、執行面が弱い。より強力な条項が求められた。

## 検討した案

- (a) AGPLv3
- (b) PolyForm Noncommercial 1.0.0
- (c) 非商用 + copyleft のカスタム条項

### AGPLv3 を却下した理由

`LICENCE.md` が「準拠」を宣言していた上流ライセンス群は、いずれも**非商用利用を条件**とする（`MMM_LICENSE.md` — LittleMaidMob by MMM、`Verclene_LICENSE.md` — LittleMaidReengaged）。AGPLv3 は商用利用を許可するライセンスであり、リポジトリ全体を AGPLv3 で配布すると、上流に対して持たない権利（商用利用の許諾）を配布者に与える形になり、上流条件と衝突する。

AGPLv3 を採るには (1) 上流権利者（MMM 原典・EMB4・Verclene・Sistr・firis-games）から書面で再ライセンス許可を得る、(2) 上流由来のコード・アセットを分離/削除する、のいずれかが必要。今回いずれも満たしていないため見送り。

補足: AGPLv3 と Minecraft / NeoForge(LGPL-2.1)・Mojang EULA の互換性も論点として残る。

### カスタム条項を却下した理由

「非商用 + ソース開示」を同時に満たす標準ライセンスは存在しない。GPL 系は追加制限を禁止する（AGPL をライセンス継承的に縛りつつ非商用化できない）ため、両立させたい場合は条項の自作が必要になる。今回要件として必須ではなかったため、標準ライセンスを選んだ。

## 決定

**PolyForm Noncommercial 1.0.0 を採用。** 非商用の核心は維持するため上流条件と両立し、かつ以下が得られる。

- `Required Notice:` 行の伝達義務（配布時に必ず著作権表示が届く）
- 違反時の 32 日治癒期間つきライセンス失効条項（執行の歯車）
- 特許ライセンス + 特許抗弁条項
- 「非商用」の明確な定義（商用/非営利組織/個人的利用の線引き）

ただしソース開示義務はない。ソース開示が必要になれば、上流許可を取って AGPL への再切替を検討する。

## 適用範囲

- 適用開始: 2026-10-10 以降のコミット、および `1.1.4` 以降のリリース
- `1.1.3` 以前のリリースは旧 LittleMaid Licence で配布済み。既に成立した許諾は切替によって失われないため、旧バージョンの複製物は旧条件のまま利用されうる
- 上流由来部分に対する上流の条件は本ライセンス下でも継続遵守（`LICENCE.md` に明記）

## 変更したファイル

- `LICENCE.md` — 適用範囲・Required Notice・上流条件継続遵守を明記し、公式プレーンテキスト全文を無改変で収録
- `gradle.properties` — `mod_license` を `PolyForm-Noncommercial-1.0.0`（SPDX 識別子）に変更。`neoforge.mods.toml` は `${mod_license}` 経由なので自動追従
- `README.md` — ライセンス表記を更新

## 残課題

- 配布プラットフォーム（Modrinth / CurseForge）の license 欄が PolyForm を選択できるか確認。選べない場合はカスタム/URL 指定での申請が必要
- 上流権利者への許可取得は未実施。AGPL 化 or 上流コード分離の判断が必要になった時点で検討する
