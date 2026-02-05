# English Word App - Product Design

**Date:** 2025-02-05
**Product Type:** Mobile App (iOS/Android)
**Design Style:** Modern Minimalist

---

## Product Overview

An English vocabulary learning app that helps users maintain a word list, interact with AI to learn new words, and practice through immersive role-play conversations.

**Target Use Case:** Learning during fragmented time (commutes, breaks, waiting periods)

---

## Core Features

### 1. Word Vault (单词库)
- Display all saved words in card format
- Show word, phonetic, meaning, and mastery level (1-5 stars)
- Search and filter by mastery status
- Tap card to view detailed information (examples, notes, practice history)

### 2. AI Assistant (AI助手)
- **Ask Mode:** Question AI about word meanings, usage, and collocations
- **Add Mode:** Tell AI a word, AI explains it and offers to add to word list
- Conversation history for review

### 3. Scene Practice (场景训练)
- Role-play conversations with AI in real-life scenarios
- Practice target words in context
- Real-time feedback on word usage
- Progress tracking (turns completed)

---

## Screen Architecture

### Screen 1: Word Vault - Main Screen
**Purpose:** Home screen displaying word collection

**Components:**
- Status bar (time, signal icons)
- Header with app title "Word Vault" and word count
- Settings button (top right)
- Search bar
- Filter tags: All / Learning / Mastered
- Word card list (scrollable)
- Floating Action Buttons:
  - AI Assistant button (top right, black circle)
  - Start Training button (bottom right, large orange circle)
- Bottom safe area with home indicator

**Word Card Content:**
- Word (bold, large)
- Phonetic transcription
- Star rating (1-5 stars)
- Brief meaning

---

### Screen 2: AI Chat - Conversation
**Purpose:** Learn new words from AI OR train selected words

**Navigation:**
- **From:** Main screen (tap AI assistant button) OR Scene Practice (via Train button)
- **To:** Main screen (tap back button) OR Scene Practice (via Train button)

**Components:**
- Status bar
- Header with back button and title "AI Learning Assistant"
- Chat area (scrollable)
  - AI avatar (black circle with robot icon)
  - Message bubbles (gray for AI, orange for user)
  - Example cards with highlighted words
  - **Action buttons** (TWO buttons, horizontal layout):
    - "Add to list" (orange) - Add word to vocabulary
    - "Train" (black) - Practice this word immediately
- Input area with text field and send button
- Bottom safe area

**Two Usage Modes:**

#### Mode 1: Word Inquiry (询问单词)
**Entry:** From Main screen (tap AI assistant button)

**Conversation Flow:**
1. AI welcomes user: "Hi! Ask me anything about English words."
2. User asks: "What does 'ephemeral' mean?"
3. AI responds with:
   - Word + phonetic
   - Definition
   - Example sentence in white card
   - Two action buttons: "Add to list" (orange) + "Train" (black)
4. User chooses:
   - **Add to list** → Word added to collection (1 star), stay in chat
   - **Train** → Jump to Scene Practice with this word

#### Mode 2: Word Training (训练单词)
**Entry:** From Main screen (multi-select + Train) OR AI Chat (after learning a word, click "Train")

**Conversation Flow:**
1. **No welcome message** - jumps straight into scenario
2. AI sets up scenario: "Let's practice 'ephemeral'! You're at a coffee shop..."
3. AI speaks as character: "Hello! What can I get you today?"
4. User responds, trying to use the target word
5. AI provides feedback and continues dialogue
6. After training completes → Go to Training Summary

**Key Differences:**

| Feature | Word Inquiry Mode | Word Training Mode |
|---------|-------------------|---------------------|
| **Entry point** | Main screen → AI button | Main screen → multi-select + Train, OR AI Chat → "Train" button |
| **Welcome message** | Yes (warm greeting) | No (straight to scenario) |
| **Action buttons** | "Add to list" + "Train" | None (already in training) |
| **Primary goal** | Learn word meaning | Practice using the word |
| **Exit to** | Main screen OR Scene Practice | Training Summary |

---

### Screen 3: Scene Practice - Role Play
**Purpose:** Practice words through immersive conversations

**Navigation:**
- **From:** Main screen (tap Start Training button)
- **To:** Training Summary (complete all turns) → Main screen

**Components:**
- Status bar
- Header with:
  - Close button (X icon)
  - Scene title (e.g., "Restaurant Scene")
  - Progress bar (e.g., "3/5 turns")
- **Target Words Section** (NEW - for batch training)
  - Flag icon + "Target Words (3)" title
  - Expandable/collapsible word list
  - Individual word tags showing:
    - Word name (bold)
    - Current mastery level (star icons)
    - Light orange background (#fff5f2)
    - Orange border (#e07a5f)
  - Example tags:
    ```
    ┌─────────────┐
    │ ephemeral ⭐⭐ │
    └─────────────┘
    ┌─────────────────┐
    │ serendipity ⭐⭐⭐ │
    └─────────────────┘
    ```
- Role card:
  - Character avatar with icon
  - Character name and greeting
- Chat area:
  - Role-play messages from AI character
  - User responses (orange bubbles)
  - Feedback messages (green border, success icon)
- Input area
- Bottom safe area

**Practice Flow:**
1. AI introduces scenario and role
2. **User reviews Target Words section** - sees all selected words with current levels
3. AI speaks as the character
4. User responds in English
5. AI provides feedback:
   - **Success:** Green bubble "Great! You used 'recommend'!"
   - **Suggestion:** Better alternative expressions
6. Continue for set number of turns (e.g., 5)
7. **Real-time progress updates** - stars animate in Target Words section as words are mastered
8. Summary screen at end

**Target Words Section Behavior:**
- **Single word training:** Shows one tag with word and current level
- **Multiple words (1-3):** Shows all tags, highlights current target word with glow effect
- **Multiple words (4+):** Shows all tags in grid layout, collapsible to save space
- **Progress tracking:** As words are mastered, star count updates in real-time with animation
- **Completed words:** Tags turn green and show checkmark when mastered during session

---

### Screen 4: Training Summary - Session Complete
**Purpose:** Show training results and celebrate progress

**Navigation:**
- **From:** Scene Practice (complete all turns)
- **To:** Word Vault (tap "Back to Home") or Scene Practice (tap "Continue Learning")

**Components:**
- Status bar
- Header with:
  - Celebration title: "Great Session!"
  - Subtitle: "You practiced 3 words and made progress!"
- Content area:
  - Progress Summary card (white background)
    - Title: "Progress Summary"
    - Word progress list:
      - Word name
      - Stars before → Stars after (with arrow)
      - Mastered badge (green) for newly mastered words
  - Achievement Unlocked card (optional)
    - Trophy icon
    - Achievement name
    - Description
- Bottom buttons:
  - "Continue Learning" (orange, primary)
  - "Back to Home" (white, secondary)
- Bottom safe area

**User Flow:**
1. Complete training session (all turns finished)
2. Show summary screen with progress
3. Celebrate achievements (if any)
4. User chooses: Continue learning or Go home

**Progress Display Example:**
```
┌────────────────────────────────┐
│ Progress Summary               │
├────────────────────────────────┤
│ ephemeral    ⭐⭐ → ⭐⭐⭐      │
│ serendipity  ⭐⭐⭐ → ⭐⭐⭐⭐   │
│ ubiquitous ⭐⭐⭐⭐ → ⭐⭐⭐⭐⭐ [Mastered!]
└────────────────────────────────┘

┌────────────────────────────────┐
│ 🏆 Achievement Unlocked!       │
│ Vocabulary Voyager             │
│ mastered 10 words              │
└────────────────────────────────┘
```

---

## Page Navigation Logic

### Design Files Structure

| Pen File | Screen Name | Purpose |
|----------|-------------|---------|
| `01-design-system.pen` | Design System Foundation | Colors, typography, components library |
| `02-word-vault-main.pen` | Word Vault - Home Screen | Home screen with word list, search, filters, **and Selection Mode** |
| `03-ai-chat.pen` | AI Chat - Conversation | Learn new words from AI OR train selected words |
| `04-scene-practice.pen` | Scene Practice - Role Play | Practice words through role-play |
| `05-training-summary.pen` | Training Summary - Session Complete | Show training results and celebrate progress |

**Note:**
- `02-word-vault-main.pen` includes **two states**: Normal Mode and Selection Mode (multi-select)
- Selection Mode is activated by long-pressing any word card (not a separate file)
- `03-ai-chat.pen` supports **two modes**: Word Inquiry (ask AI) and Word Training (practice with selected words)
- Each pen file represents one screen with its possible states

### Complete Navigation Flow

```
┌─────────────────────────────────────────────┐
│         Word Vault (Home)                   │
│  搜索、筛选、多选单词                        │
│  Quick actions:                             │
│    • AI Assistant (top-right) → 学习新词    │
│    • Batch Train (底部浮动) → 批量训练     │
└─────────────────────────────────────────────┘
                    │
        ┌───────────┴───────────┐
        │                       │
        ▼                       ▼
┌───────────────┐       ┌──────────────┐
│  AI Chat      │       │Scene Practice│
│  询问单词     │       │- 角色扮演    │
│  添加到列表   │       │- 针对训练    │
│  (单个单词)   │       │- (多选单词)  │
│  或直接训练   │       └──────────────┘
└───────────────┘              │
        │                       │
        │                       ▼
        │               ┌──────────────┐
        │               │Training      │
        │               │Summary       │
        │               │- 成就展示    │
        │               │- 进度总结    │
        │               └──────────────┘
        │                       │
        │         ┌─────────────┬─┴──────────┐
        │         ▼             ▼             ▼
        │    Continue      Back to      练习数据
        │    Learning      Home         已保存
        │         │             │
        │         └──────┬───────┘
        │                │
        └────────────────┘

可选路径：AI Chat → Add word → 直接训练
                  └→ Train selected words → Scene Practice
```

### Navigation States

**Entry Point:**
- App opens → Word Vault (Home)

**Primary Actions:**

1. **学习新单词（单个）**
   - Tap AI button (top-right) → AI Chat
   - 询问单词 → AI解释 → 点击"添加到单词列表"
   - 新单词自动添加为 "Learning" 状态（1星）

2. **批量训练（多个单词）**
   - 长按卡片进入多选模式
   - 选择多个单词 → 点击"Train X words"浮动按钮
   - 进入 Scene Practice → 针对选中的单词进行训练
   - 训练完成后返回，更新所有选中单词的掌握度

3. **查看单词详情**
   - Tap word card → 展开详情
   - 再次点击 → 收起

4. **快速训练（随机）**
   - Tap "Start Training" (bottom-right, 大橙色按钮)
   - 自动选择需要复习的单词（Learning状态）
   - 进入 Scene Practice

5. **单词状态管理**
   - Learning → Mastered: 练习时成功使用，累计达到5星
   - Mastered → Learning: 长按卡片 → "重新学习"选项

**筛选功能：**
- **All**: 显示所有单词（Learning + Mastered）
- **Learning**: 仅显示学习中单词（1-4星）
- **Mastered**: 仅显示已掌握单词（5星）

### Detailed Page Transitions

#### 1. Word Vault → AI Chat
**Trigger:** Tap AI Assistant button (black circle, top-right)

**UI Element:**
- Location: 02-word-vault-main.pen
- Button type: Floating action button
- Icon: robot/smart_toy icon
- Position: Fixed top-right overlay

**Data Passed:**
- None (new conversation)

**User Flow:**
```
Word Vault (Home)
    ↓ [tap AI button]
AI Chat Screen
    ↓ [ask about word → AI explains → tap "Add to my word list"]
    ↓ [word added to list with masteryLevel=1, status="learning"]
Word Vault (automatically returns, shows new word)
```

#### 2. Word Vault → Scene Practice (Quick Training)
**Trigger:** Tap "Start Training" button (large orange circle, bottom-right)

**UI Element:**
- Location: 02-word-vault-main.pen
- Button type: Floating action button
- Style: Primary orange, large size
- Position: Fixed bottom-right overlay

**Data Passed:**
- Auto-selected words: All words with status="learning"
- Training mode: "auto" (random selection)

**User Flow:**
```
Word Vault
    ↓ [tap "Start Training"]
Select words automatically (all Learning status words)
    ↓
Scene Practice
    ↓ [Target Words shows selected words]
    ↓ [complete training session]
    ↓ [update mastery levels, mark mastered if reach 5 stars]
Word Vault (shows updated progress)
```

#### 3. Word Vault → Selection Mode → Scene Practice (Batch Training)
**Trigger:** Long press any word card

**UI Element:**
- Location: 02-word-vault-main.pen (Selection Mode state)
- Gesture: Long press (500ms)
- Feedback: Haptic vibration (Android 15)
- State Change: Screen transitions from Normal Mode to Selection Mode

**Data Passed:**
- Selected word IDs: Array of word IDs
- Training mode: "batch" (user-selected)

**User Flow:**
```
Word Vault (normal mode)
    ↓ [long press card → state change]
Selection Mode activates (same screen, different state)
    ↓ [select multiple words]
    ↓ [tap "Train X words" button]
Scene Practice
    ↓ [Target Words section shows all selected words with stars]
    ↓ [complete training across multiple turns]
    ↓ [update all selected words' mastery levels]
Word Vault (shows updated progress for all words)
```

**Selection Mode UI Changes (in 02-word-vault-main.pen):**
- Header: "Cancel" | "Selected 2/12" | "Select All"
- Search bar: Disabled (grayed out)
- Word cards: Show checkboxes (orange fill=selected, white empty=unselected)
- Bottom button: "Train 2 words" (replaces "Start Training")
- Exit: Tap "Cancel" to return to Normal Mode

#### 4. Scene Practice → Word Vault (Complete Training)
**Trigger:** Tap close button (X) or complete all turns

**UI Element:**
- Location: 04-scene-practice.pen
- Button type: Header close button
- Icon: close icon
- Position: Top-left in header

**Data Passed Back:**
```typescript
{
  sessionId: string,
  results: {
    wordId: string,
    levelBefore: number,
    levelAfter: number,
    mastered: boolean  // true if reached 5 stars
  }[],
  completedAt: Date
}
```

**User Flow:**
```
Scene Practice (in progress)
    ↓ [complete all turns OR tap close button]
    ↓ [calculate progress updates]
    ↓ [show summary screen with results]
Word Vault (automatically refreshes word list with new levels)
```

#### 5. AI Chat → Word Vault (Back Navigation)
**Trigger:** Tap back button (top-left)

**UI Element:**
- Location: 03-ai-chat.pen
- Button type: Header back button
- Icon: arrow_back_ios_new
- Position: Top-left in header

**Data Passed Back:**
- Optional: New word added (if user tapped "Add to my word list")

**User Flow:**
```
AI Chat
    ↓ [tap back button]
Word Vault (returns home, shows updated word count)
```

### Page State Management

**Navigation Stack Behavior:**
- Word Vault is always the root (bottom of stack)
- AI Chat and Scene Practice are pushed onto stack
- Back button pops current screen and returns to previous
- All screens maintain their own state (scroll position, input text, etc.)

**Data Persistence:**
- Word list: Saved to local storage immediately
- Conversation history: Saved per session
- Selection state: Reset when exiting selection mode
- Training progress: Updated in real-time, saved on completion

---

## Word Status & Mastery System

### Word Status States

单词有三种状态，基于掌握程度（masteryLevel）自动转换：

| Status | Mastery Level | 触发条件 | 显示筛选 |
|--------|---------------|----------|----------|
| **Learning** | 1-4 stars | • 新添加的单词<br>• 练习中但未达到5星<br>• 主动标记"重新学习" | Learning 标签 |
| **Mastered** | 5 stars | • 练习中成功使用目标词<br>• 累计达到5星 | Mastered 标签 |
| **All** | 1-5 stars | 所有单词 | All 标签（默认） |

### Status Transition Logic

```
                    ┌─────────────┐
                    │   New Word  │
                    │  (AI添加)   │
                    └──────┬──────┘
                           │
                           ▼
                    ┌─────────────┐
                    │  Learning   │
                    │  (1 star)    │◄─────────────┐
                    └──────┬──────┘              │
                           │                     |
                           │ 练习成功              │ 用户主动
                           │ (masteryLevel +1)    │ "重新学习"
                           ▼                     │
                    ┌─────────────┐              │
                    │  Learning   │              │
                    │  (2-4 stars) │──────────────┘
                    └──────┬──────┘
                           │
                           │ 练习成功
                           │ (达到5星)
                           ▼
                    ┌─────────────┐
                    │  Mastered   │
                    │  (5 stars)   │
                    └─────────────┘
```

### Practice & Progress Update

**场景训练中的进度更新：**

1. **开始训练前**
   - 用户选择N个单词（或自动选择需要复习的）
   - Scene Practice 显示："Target words: [word1, word2, ...]"

2. **训练过程中**
   - AI引导对话，创造使用目标词的场景
   - 用户回复使用目标词
   - AI检测并反馈

3. **每轮对话后的反馈**
   - ✅ **成功使用**: "Great! You used 'recommend'!"
     - 所有相关单词的 masteryLevel +1
     - 达到5星的单词自动转为 "Mastered"
   - 💡 **建议改进**: "Try saying: ..."
     - masteryLevel 不变
     - 提供更好的表达方式

4. **训练完成总结**
   - 显示所有参与训练的单词
   - 每个单词的进度变化
   - 掌握的新单词数量
   - 返回主屏幕，自动刷新筛选状态

### Example Flow

**场景: 用户选择3个Learning状态的单词进行训练**

1. **主屏幕**: 长按 ephemeral (2星) → 多选模式
2. **选择**: serendipity (3星), ubiquitous (4星)
3. **点击**: "Train 3 words" 浮动按钮
4. **进入**: Scene Practice (Restaurant场景)
5. **目标提示**: "Target words: ephemeral, serendipity, ubiquitous"
6. **训练3轮对话**:
   - Round 1: 成功使用 ephemeral → ephemeral 2→3星
   - Round 2: 成功使用 serendipity → serendipity 3→4星
   - Round 3: 成功使用 ubiquitous → ubiquitous 4→5星 ⭐
7. **完成**: 显示总结
   - ✅ ubiquitous 掌握！ (Mastered)
   - 📊 ephemeral, serendipity 进度提升
8. **返回**: 主屏幕自动切换到 "All" 或 "Mastered" 查看结果

---

## Design System

### Colors
- **Background:** #faf9f7 (warm white)
- **Foreground:** #1a1a1a (dark charcoal)
- **Muted:** #6b6b6b (gray)
- **Primary:** #e07a5f (terracotta orange)
- **Success:** #81b29a (sage green)
- **Border:** #e5e5e5 (light gray)

### Typography
- **Display:** Inter, 28-32px, bold (titles)
- **Body:** Inter, 14-15px, regular (content)
- **Caption:** Inter, 12-13px (labels, hints)

### Components

**Buttons:**
- Primary: Orange fill, white text, rounded pill
- Secondary: White fill, border, dark text
- Icon buttons: 40-48px, circle or rounded square

**Cards:**
- 16px corner radius
- White background
- Subtle border
- 12-16px padding

**Chat Bubbles:**
- AI: Gray (#f5f5f5), left-aligned
- User: Orange (#e07a5f), right-aligned
- 16px corner radius with one flat side
- 12px padding

**Progress Indicators:**
- Thin bar (6px height)
- Gray background
- Orange fill
- Rounded ends

---

## Interaction Patterns

### Gestures
- **Tap** on card: Expand to view details (or select in selection mode)
- **Long press** card: Enter selection mode (multi-select)
- **Swipe** card: Quick actions (delete, relearn)
- **Pull down** on list: Refresh

### Selection Mode (Multi-Select)

**进入多选模式：**
1. 长按任意单词卡片 → 进入多选模式
2. 或点击顶部的"选择"按钮（如有）

**多选模式UI变化：**
- 顶部标题变化：显示"Selected X/12"
- 新增操作按钮：
  - ✅ 全选/取消全选
  - ❌ 取消选择（退出多选模式）
- 单词卡片：
  - 显示选择框（checkbox）
  - 已选：橙色勾选标记
  - 未选：灰色边框
- 底部浮动按钮变化：
  - 原来："Start Training"按钮隐藏
  - 新增："Train X words" 按钮（显示选中数量）

**选择操作：**
- 点击卡片：切换选中状态
- 点击"全选"：选中当前筛选下的所有单词
- 点击"Train X words"：进入训练
- 点击空白处/取消按钮：退出多选模式

**从训练进入Scene Practice：**
- 单选训练：直接进入，显示该单词
- 多选训练（1-3个）：每轮对话随机引导使用其中一个
- 多选训练（4+个）：多轮对话，确保覆盖所有单词

### Feedback
- **Success:** Green color, checkmark icon, "Great! You used X!"
- **Suggestion:** Yellow accent, lightbulb icon, "Try saying..."
- **Progress Update:** Star icons animate, mastery level changes
- **Selection Feedback:**
  - 进入多选：轻微震动反馈（安卓15 HapticFeedback）
  - 选中卡片：勾选图标动画
  - 完成选择："Train X words"按钮从底部滑入

### Loading States
- **Initial Load:** Skeleton screens for word cards
- **Chat Loading:** Three-dot typing indicator
- **Practice Loading:** "Preparing your practice session..."
- **Saving:** "Updating your progress..."

### Empty States
- **No Words:** "Start learning! Ask AI about any word."
- **No Selection:** "Select words to practice together"
- **No Learning Words:** "All words mastered! 🎉"
- **No Mastered Words:** "Keep practicing to master words"

### Micro-interactions
- Button press: Scale down slightly (0.95)
- Card appear: Fade in + slide up
- New message: Slide in from side
- Progress update: Smooth animate width

---

## Animation & Motion Design

### Design Philosophy
**Core Principles:**
- **Warm & Encouraging:** Animations should feel rewarding and positive
- **Non-intrusive:** Motion should enhance, not distract from learning
- **Fast & Fluid:** All animations under 400ms for responsive feel
- **Clear Feedback:** Every interaction gets visual confirmation
- **Progressive Disclosure:** Use motion to guide attention naturally

**Animation Timing:**
- **Fast:** 150-200ms (button taps, hover states)
- **Medium:** 250-300ms (card expansions, list items)
- **Slow:** 400-500ms (page transitions, success celebrations)

**Easing Functions:**
```css
--ease-out-back: cubic-bezier(0.34, 1.56, 0.64, 1)     /* Bouncy, playful */
--ease-out-quart: cubic-bezier(0.25, 1, 0.5, 1)       /* Smooth deceleration */
--ease-in-out-quart: cubic-bezier(0.76, 0, 0.24, 1)    /* Smooth acceleration */
--ease-spring: cubic-bezier(0.68, -0.55, 0.265, 1.55)  /* Springy pop */
```

---

### 1. Page Transitions (页面转场动画)

#### A. Word Vault → AI Chat
**Animation Type:** Modal slide-up with scale

**Sequence:**
```
1. User taps AI button
2. AI button: scale 0.95 → 1.05 (100ms) → scale 1 (spring)
3. Screen dims: rgba(0,0,0,0) overlay 0 → 0.3 (200ms, ease-out-quart)
4. AI Chat screen:
   - slide in from bottom: translateY(100%) → 0 (350ms, ease-out-back)
   - scale in: scale(0.9) → scale(1) (350ms, ease-out-back)
   - elements stagger in:
     * Header: delay 50ms
     * Chat area: delay 100ms (fade in + slide up 20px)
     * Input area: delay 150ms (slide up from bottom 40px)
```

**Effect:** AI Chat feels like it's "popping up" from the bottom, welcoming and accessible

---

#### B. Word Vault → Scene Practice
**Animation Type:** Full-screen slide with scale

**Sequence:**
```
1. User taps "Start Training" or "Train X words"
2. Floating button: scale 1 → 0.8 (200ms, ease-in-out-quart)
3. Word Vault: scale 1 → 0.95, opacity 1 → 0.8 (250ms, ease-in)
4. Scene Practice slides in from right:
   - translateX(100%) → translateX(0) (400ms, ease-out-back)
   - scale: 0.9 → 1 (400ms, ease-out-back)
5. Scene Practice elements stagger in:
   * Header & Target Words: delay 100ms
   * Role card: delay 150ms (pop in from left)
   * Chat messages: delay 200ms (staggered 50ms each)
```

**Effect:** Smooth transition feels like "stepping into" a practice scenario

---

#### C. Scene Practice → Training Summary
**Animation Type:** Celebration reveal

**Sequence:**
```
1. Training completes
2. Scene Practice: fade out, scale 0.95 (300ms, ease-in)
3. Training Summary reveals with celebration:
   - Title "Great Session!":
     * scale 0 → 1.2 → 1 (400ms, ease-out-back)
     * bounce effect on completion
   - Progress Summary card:
     * slide up from bottom 60px (300ms, delay 100ms, ease-out-back)
     * fade in 0 → 1 (300ms, delay 100ms)
   - Word progress items stagger in: delay 50ms each
   - Achievement card (if unlocked):
     * confetti burst effect (optional, 500ms)
     * card pop in with rotation: scale 0.8 → 1.1 → 1 (400ms, ease-spring)
   - Buttons slide up from bottom 80px: delay 300ms
```

**Effect:** Big reveal moment with celebratory feel, dopamine hit for user

---

#### D. AI Chat → Scene Practice (via Train button)
**Animation Type:** Quick transition with context carry

**Sequence:**
```
1. User clicks "Train" button
2. Chat area: scale 0.95, blur 4px (200ms)
3. Scene Practice slides in:
   - cover from right: translateX(100%) → 0 (350ms, ease-out-quart)
   - fade in: opacity 0 → 1 (350ms, ease-out-quart)
4. Target word card floats from AI Chat:
   - position: center of previous screen → target position (400ms, ease-out-back)
   - scale: 0.8 → 1 (400ms, ease-out-back)
5. Chat messages fade out (200ms)
```

**Effect:** Seamless flow, user feels the word they just learned is "carried over" to practice

---

#### E. Back Navigation (all screens → Word Vault)
**Animation Type:** Reverse of entry + subtle parallax

**Sequence:**
```
1. User taps back button
2. Current screen:
   - slide out: direction depends on entry
   - scale 1 → 1.05 (150ms, ease-in)
   - fade out: opacity 1 → 0 (250ms, ease-in)
3. Word Vault reveals:
   - scale: 1.02 → 1 (200ms, ease-out-quart)
   - fade in: opacity 0.8 → 1 (200ms, ease-out-quart)
   - word cards stagger in: delay 20ms each (pop up from below)
```

**Effect:** Quick, responsive return to home, not sluggish

---

### 2. Button Interactions (按钮交互动画)

#### A. Primary Buttons (orange FABs, "Add to list", "Train")

**Hover/Press States:**
```css
/* Hover (desktop only) */
.button:hover {
  transform: scale(1.05);
  box-shadow: 0 8px 20px rgba(224, 122, 95, 0.4);
  transition: all 200ms ease-out-quart;
}

/* Press/Touch */
.button:active {
  transform: scale(0.95);
  box-shadow: 0 2px 8px rgba(224, 122, 95, 0.3);
  transition: all 100ms ease-in-out-quart;
}
```

**Release Feedback:**
```
- Scale: 0.95 → 1.05 (spring, 150ms)
- Then: 1.05 → 1 (decay, 100ms)
- Creates: Satisfying "pop" feeling
```

#### B. Secondary Buttons ("Back to Home", "Cancel")

**Subtler Feedback:**
```css
.button:active {
  transform: scale(0.98);
  background: #f0f0f0;
  transition: all 150ms ease-out-quart;
}
```

#### C. Icon Buttons (AI assistant, settings)

**Rotation on Entry:**
```
- Icon rotates: 360deg (400ms, ease-out-back)
- While rotating: scale 0.9 → 1.1 → 1
- Creates: Playful, dynamic feel
```

---

### 3. Card Animations (卡片动画)

#### A. Word Card - Selection Mode Toggle

**Enter Selection Mode (long press):**
```
1. User long presses card (500ms hold detected)
2. Haptic feedback (Android 15)
3. Card transforms:
   - scale: 1 → 1.02 → 1 (300ms, ease-spring)
   - border: 1px → 2px orange (200ms)
   - checkbox fades in: scale 0 → 1 (200ms, delay 150ms)
   - checkbox pop: scale 1.2 → 1 (100ms, delay 200ms)
4. Other cards:
   - scale: 1 → 0.98 (200ms)
   - opacity: 1 → 0.7 (200ms)
   - Creates focus on selected card
```

**Toggle Selection (tap):**
```
Selected → Deselected:
  - Border: 2px orange → 1px gray (200ms)
  - Checkbox: scale 1 → 0 → disappear (150ms)
  - Card: slight bump scale 1.02 → 1 (200ms, ease-spring)

Deselected → Selected:
  - Border: 1px gray → 2px orange (200ms)
  - Checkbox: scale 0 → 1 → 1.2 → 1 (300ms, ease-spring)
  - Checkmark: stroke-dashoffset animation (200ms)
```

#### B. Word Card - Swipe Actions

**Swipe Reveal:**
```
1. User swipes card left/right
2. Card follows finger (translateX)
3. At 60px threshold: actions reveal from behind
   - Left action (relearn): slides in from right (200ms)
   - Right action (delete): slides in from left (200ms)
4. Background color: tint red (delete) / green (relearn)
5. Release:
   - If <60px: snap back (spring, 300ms)
   - If >60px: action triggers, card dismissed (400ms)
```

---

### 4. Chat Animations (聊天动画)

#### A. Message Bubbles - Appearance

**AI Message (left):**
```
1. Typing indicator shows first (3 dots bounce, 400ms)
2. Message slides in:
   - translateX: -20px → 0 (250ms, ease-out-back)
   - opacity: 0 → 1 (250ms, ease-out-back)
   - scale: 0.95 → 1 (250ms, ease-out-back)
3. Content fades in: delay 100ms, 150ms
```

**User Message (right):**
```
1. User taps send
2. Input fades out: opacity 1 → 0 (100ms)
3. Message appears:
   - translateX: 20px → 0 (250ms, ease-out-back)
   - opacity: 0 → 1 (250ms, ease-out-back)
   - scale: 0.95 → 1 (250ms, ease-out-back)
4. Input fades back in: delay 300ms, 150ms
```

#### B. Action Buttons - Appearance

**"Add to list" + "Train" buttons:**
```
1. Appear after AI message: delay 300ms
2. Container: scale 0.8 → 1 (300ms, ease-spring)
3. Buttons stagger:
   - "Add to list": delay 0ms
   - "Train": delay 50ms
4. Both buttons:
   - slide up: translateY(20px) → 0 (300ms, ease-out-back)
   - fade in: opacity 0 → 1 (300ms)
```

---

### 5. Progress & Achievement Animations (进度与成就动画)

#### A. Star Rating - Level Up

**When mastery level increases:**
```
1. Old stars glow: scale 1 → 1.2 (150ms, ease-out-back)
2. Old stars fade out: opacity 1 → 0 (200ms)
3. New stars fade in: opacity 0 → 1 (200ms)
4. New stars pop: scale 0.8 → 1.3 → 1 (300ms, ease-spring)
5. Celebration burst (if reaching 5 stars):
   - Confetti particles emit from stars (500ms)
   - Screen shake: translateX/Y ±2px (3x, 50ms each)
   - Badge "Mastered!" scales in: pop effect (400ms)
```

#### B. Achievement Unlocked

**Achievement card reveal:**
```
1. Trophy icon drops in:
   - translateY(-40px) → 0 (400ms, ease-out-back)
   - rotation: -15deg → 0deg (400ms)
   - bounce: scale 1.2 → 1 (100ms)
2. Card body slides in from left:
   - translateX(-30px) → 0 (300ms, delay 100ms)
   - fade in simultaneously
3. Shine effect sweeps across:
   - gradient: translateX(-100%) → 100% (800ms, linear)
   - overlay opacity: 0 → 0.3 → 0 (800ms)
4. Confetti (optional):
   - Particles burst from trophy (600ms)
```

#### C. Target Words Section - Progress Update

**Real-time star update during training:**
```
1. User successfully uses word
2. AI shows green feedback bubble
3. Target Words tag updates:
   - Old star: scale 1 → glow (100ms)
   - Old star: fade out (150ms)
   - New star: fade in + scale 1.2 → 1 (200ms, ease-spring)
   - Tag border: flashes #81b29a (success color) (200ms)
4 - Mastered badge (if applicable):
   - Badge scales in with pop (300ms)
   - Confetti mini-burst (200ms)
```

---

### 6. Loading States (加载状态动画)

#### A. Initial App Load

**Word Cards Skeleton:**
```
1. Screen fades in: opacity 0 → 1 (300ms)
2. Status bar: slide down (200ms)
3. Header: slide down + fade in (250ms, delay 50ms)
4. Search bar: slide down + fade in (250ms, delay 100ms)
5. Filter tags: stagger in (delay 150ms, 50ms between each)
6. Word cards:
   - Skeleton pulse: opacity 0.6 → 1 → 0.6 (1.5s, infinite)
   - Staggered appearance: delay based on position (50ms increments)
   - Each card: slide up 20px + fade in (300ms, ease-out-back)
```

#### B. Chat Typing Indicator

**Three-dot bounce:**
```css
@keyframes bounce {
  0%, 100% { transform: translateY(0); }
  50% { transform: translateY(-8px); }
}

.dot:nth-child(1) { animation: bounce 600ms infinite; }
.dot:nth-child(2) { animation: bounce 600ms infinite 200ms; }
.dot:nth-child(3) { animation: bounce 600ms infinite 400ms; }
```

**Smooth fade out when message arrives:**
```
- Typing indicator: opacity 1 → 0 (200ms)
- Message: fade in 0 → 1 (200ms, delay 100ms)
```

---

### 7. Micro-Interaction Details (细节微交互)

#### A. Floating Action Button - Ripple Effect

**On tap:**
```
1. User touches button
2. Ripple circle expands from touch point:
   - scale: 0 → 2 (400ms, ease-out-quart)
   - opacity: 0.5 → 0 (400ms)
3. Button itself: scale 1 → 0.95 → 1.02 → 1
4. Ripple color: rgba(255,255,255,0.3)
```

#### B. Progress Bar - Smooth Fill

**Training progress bar:**
```
- Current: 3/5 turns
- Updates to: 4/5
- Animation:
  * Width: 60% → 80% (500ms, ease-out-quart)
  * While filling: subtle pulse on bar
  * Text count: quick scale 1 → 1.2 → 1 (200ms)
```

#### C. Checkbox - Selection Mode

**Checkmark draw animation:**
```
1. Checkbox selected: scale 0.8 → 1 (200ms)
2. Checkmark path: stroke-dashoffset animation (300ms)
3. Checkmark: scale 1.5 → 1 (100ms, spring)
4. Orange fill: fades in behind (200ms)
```

---

### 8. Scene-Specific Animation Flows

#### A. AI Chat - Word Inquiry Mode

**Entry:**
```
1. Slide up from bottom (350ms)
2. AI avatar bounces in: scale 0 → 1.2 → 1 (400ms)
3. Welcome message types out:
   - Typing indicator: 400ms
   - Message appears: 250ms slide in
4. Quick action suggestions fade in below: delay 500ms, 200ms stagger
```

**Quick Action Suggestions:**
```
┌────────────────────────────┐
│ Try asking:                 │ ← Stagger fade in
│ • "What does serendipity     │
│    mean?"                   │
│ • "Give me examples of      │
│    ephemeral"                │
└────────────────────────────┘
```

#### B. Scene Practice - Training Mode

**Entry:**
```
1. No welcome message - straight to scenario
2. Scene title fades in: 200ms
3. Target Words section slides down: translateY(-20px) → 0 (250ms)
4. Role card pops in from left:
   - translateX(-50px) → 0 (300ms, ease-out-back)
   - scale 0.9 → 1.05 → 1 (300ms)
5. Character icon pulses: scale 1 → 1.1 → 1 (infinite, 2s cycle)
```

**Progress bar fills:**
```
- Training starts: 0/5
- Each turn completes: bar advances by 20%
- Smooth animated fill: 500ms per increment
- Text updates with scale pop: 1 → 1.3 → 1 (200ms)
```

#### C. Training Summary - Celebration

**Big moment design:**
```
1. Screen transition: Scene Practice fades out
2. Title "Great Session!" bursts in:
   - scale: 0 → 1.5 → 1 (600ms, exaggerated ease-spring)
   - Confetti falls from top (500ms, random delays)
3. Progress Summary card:
   - slide up: translateY(100px) → 0 (400ms)
   - shadow grows in: scale 0.8 → 1 (400ms)
4. Each word progress:
   - Staggered delay: 100ms increments
   - Old stars: collapse (scale 0.5)
   - Arrow: draws in (stroke-dashoffset, 300ms)
   - New stars: explode in (scale 0 → 1.4 → 1, ease-spring)
   - If mastered: badge spins in (rotate 360deg, 400ms)
5. Achievement card:
   - If unlocked: major celebration (confetti, screen shake, etc.)
6. Buttons slide up from bottom: delay 400ms
```

---

### 9. Performance Optimization

**Animation Best Practices:**
- Use `transform` and `opacity` only (GPU-accelerated)
- Avoid animating `width`, `height`, `top`, `left`
- Use `will-change` sparingly (only on actively animating elements)
- Prefer CSS transitions over JS animations
- Use `requestAnimationFrame` for complex sequences

**CSS Variables for Consistency:**
```css
:root {
  --anim-fast: 150ms;
  --anim-medium: 250ms;
  --anim-slow: 400ms;
  --ease-bounce: cubic-bezier(0.34, 1.56, 0.64, 1);
  --ease-smooth: cubic-bezier(0.25, 1, 0.5, 1);
  --ease-spring: cubic-bezier(0.68, -0.55, 0.265, 1.55);
}
```

**Platform Considerations:**
- **iOS:** Use `UIVIewPropertyAnimator` for native-feeling animations
- **Android:** Use `MotionLayout` for complex transitions
- **Web:** Use CSS + `requestAnimationFrame` for 60fps

---

## Technical Notes

### API Integration
- **AI Provider:** Zhipu AI (智谱AI)
- **API Key:** Pre-configured in code (note: security consideration for production)
- **Endpoints:**
  - Chat completion for word explanations
  - Role-play conversation with context

### Data Structure
```typescript
interface Word {
  id: string
  word: string
  phonetic: string
  definition: string
  examples: string[]
  masteryLevel: number // 1-5 stars
  status: 'learning' | 'mastered' // computed from masteryLevel
  practiceCount: number
  lastPracticed: Date
  notes?: string
  selected?: boolean // for batch selection
  createdAt: Date
  updatedAt: Date
}

interface Conversation {
  id: string
  type: 'inquiry' | 'practice'
  mode: 'single' | 'batch' // single word or multiple words
  messages: Message[]
  targetWords: string[] // words to practice in this session
  progress: {
    current: number // current turn (1-based)
    total: number // total turns in this session
    completed: boolean // session completed
  }
  results?: {
    wordId: string
    levelBefore: number
    levelAfter: number
    mastered: boolean // newly mastered in this session
  }[]
  startedAt: Date
  completedAt?: Date
}

interface Message {
  id: string
  role: 'user' | 'assistant'
  content: string
  timestamp: Date
  feedback?: {
    type: 'success' | 'suggestion'
    message: string
    targetWordIds: string[] // which words this feedback applies to
  }
  usedWords?: string[] // words successfully used in this message
}

interface SelectionState {
  isSelectionMode: boolean
  selectedWordIds: string[]
  selectAll: boolean // for select all / deselect all
}
```

### State Management
- Local storage for word list
- Session state for active conversations
- Progress tracking across practice sessions

---

## Future Enhancements

### Phase 2 Features
- Word of the day notifications
- Spaced repetition scheduling
- Pronunciation practice with speech recognition
- Achievements and streaks
- Social features (share lists, compete)

### Phase 3 Features
- Multi-language support
- Offline mode
- Export to Anki/flashcard apps
- Advanced analytics dashboard
- Custom scenarios creation

---

## Design Files

### Pen Files (UI Designs)

| File | Screen Name | Size | Description |
|------|-------------|------|-------------|
| `01-design-system.pen` | Design System Foundation | - | Colors, typography, components library |
| `02-word-vault-main.pen` | Word Vault - Home Screen | 393x852 | Main screen with word list, search, filters, **and Selection Mode** (activated by long-press) |
| `03-ai-chat.pen` | AI Chat - Conversation | 393x852 | Chat interface for learning new words OR training selected words |
| `04-scene-practice.pen` | Scene Practice - Role Play | 393x852 | Training interface with Target Words section |
| `05-training-summary.pen` | Training Summary - Session Complete | 393x852 | Shows training results, progress updates, and unlocked achievements |

**Device Spec:** iPhone 14 Pro (393x852) - responsive design adapts to all screen sizes

**Screen States:**
- **02-word-vault-main.pen** has two states:
  - Normal Mode: View and search words
  - Selection Mode: Multi-select words for batch training (triggered by long-press)
- **03-ai-chat.pen** has two modes:
  - Word Inquiry: Ask AI about word meanings and usage
  - Word Training: Practice selected words directly (no welcome message, jumps straight to scenario)

### Documentation

- **Product Design:** `docs/2025-02-05-english-word-app-design.md` (this file)
  - Complete feature specifications
  - Page navigation logic
  - Word status & mastery system
  - Data structures & APIs
  - Design system guidelines

### Backup Files

- `english-word-app-ui.html` - Interactive HTML preview of all screens

---

## Summary

This English Word App combines three powerful learning mechanisms:
1. **Vocabulary Management:** Clean, organized word library
2. **AI-Powered Learning:** Instant explanations and context
3. **Immersive Practice:** Role-play scenarios for real-world usage

The modern minimalist design ensures focus on content while maintaining visual appeal. The mobile-first approach with large touch targets and clear navigation makes it perfect for learning during short breaks throughout the day.
