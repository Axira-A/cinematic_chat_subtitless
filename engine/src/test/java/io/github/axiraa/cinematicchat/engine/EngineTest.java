package io.github.axiraa.cinematicchat.engine;

import io.github.axiraa.cinematicchat.api.*;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class EngineTest {
    private final CcmlParser parser = new CcmlParser();
    private SubtitleRuntime runtime(String text) { return new SubtitleRuntime(1, new SubtitleTimeline().compile(parser.parse(text), EngineConfig.DEFAULT)); }
    private String rendered(String text) { var r = runtime(text); r.advance(1000, e -> {}); return r.snapshot().text(); }
    @Test void olderEntriesMoveContinuouslyDuringEntryAndExit(){
        var old=new SubtitleStackLayout.Item(1,20,1,0);double previous=Double.POSITIVE_INFINITY;
        for(int frame=0;frame<=20;frame++){
            double progress=frame/20.0;var layout=SubtitleStackLayout.place(List.of(old,new SubtitleStackLayout.Item(2,30,progress,0)),100,1,8,Animation.Easing.LINEAR,false);
            double top=layout.getLast().top();assertEquals(80-38*progress,top,1e-9);assertTrue(top<=previous);previous=top;
        }
        assertEquals(80,SubtitleStackLayout.place(List.of(old,new SubtitleStackLayout.Item(2,30,1,1)),100,1,8,Animation.Easing.LINEAR,false).getLast().top());
        assertEquals(42,SubtitleStackLayout.place(List.of(old,new SubtitleStackLayout.Item(2,30,0,0)),100,1,8,Animation.Easing.LINEAR,true).getLast().top());
    }
    @Test void escapesAndNewline() { assertEquals("这是[pause=5]普通文字。\n\\", rendered("这是\\[pause=5\\]普通文字。\\n\\\\")); }
    @Test void everyParsedNodeRetainsItsExactSourceRange(){
        String input="a[color=#ff0000]\\n[/color][pause=1][br]";var doc=parser.parse(input);
        assertEquals("a",input.substring(doc.nodes().getFirst().range().start(),doc.nodes().getFirst().range().end()));
        var span=(SubtitleNode.Span)doc.nodes().get(1);assertEquals("[color=#ff0000]\\n[/color]",input.substring(span.range().start(),span.range().end()));
        var escaped=span.children().getFirst();assertEquals("\\n",input.substring(escaped.range().start(),escaped.range().end()));
        assertEquals("[pause=1]",input.substring(doc.nodes().get(2).range().start(),doc.nodes().get(2).range().end()));
        assertEquals("[br]",input.substring(doc.nodes().get(3).range().start(),doc.nodes().get(3).range().end()));
    }
    @Test void nestedStylesRetainBaseStyleAndLocalOverrides() {
        var source = new StyledText("不要[color=#B02020][shake]碰[/shake][/color]。", List.of(new StyledText.Run(0, 100, new TextStyle(0x00ff00, "test:font", false, true, false, false, false, 1, 1, 0, 12, 0, 2))));
        var r = new SubtitleRuntime(1, new SubtitleTimeline().compile(parser.parse(source, CcmlLimits.ABSOLUTE, true), EngineConfig.DEFAULT)); r.advance(10, e -> {});
        assertEquals("不要碰。", r.snapshot().text());
        var style = r.snapshot().glyphs().get(2).glyph().style();
        assertEquals(0xB02020, style.color()); assertEquals("test:font", style.font()); assertTrue(style.italic()); assertTrue(style.shake() > 0);
        assertEquals(0x00ff00, r.snapshot().glyphs().getFirst().glyph().style().color());
    }
    @Test void malformedAndUnknownRemainLiteral() {
        for (String input : List.of("hello [pause=abc] world", "[color=#GGHHZZ]", "[test]", "[shake][wave][/color]", "[pause=NaN]", "[pause=Infinity]", "[erase=1.5]")) assertEquals(input, rendered(input));
    }
    @Test void numericCeilingsCannotBeRaisedByConfig() {
        var limits = new CcmlLimits(Integer.MAX_VALUE, Integer.MAX_VALUE, Integer.MAX_VALUE, 1000, 1000, -10, 1000, 1000, 1000, 9999).bounded();
        assertEquals(CcmlLimits.ABSOLUTE, limits);
        var r = runtime("a[pause=999999999]b"); assertTrue(r.completeAt() < 11);
        assertEquals("ab", rendered("a[pause=999999999]b"));
    }
    @Test void unicodeUnitsNeverSplitClusters() {
        for (String text : List.of("中文", "日本語", "한국어", "😀", "e\u0301", "👨‍👩‍👧‍👦", "🇨🇳", "👍🏽")) {
            assertEquals(text, UnicodeUnits.split(text).stream().map(UnicodeUnits.Unit::text).reduce("", String::concat));
        }
        assertEquals(1, UnicodeUnits.split("👨‍👩‍👧‍👦").size()); assertEquals(1, UnicodeUnits.split("e\u0301").size());
        assertEquals("", rendered("👨‍👩‍👧‍👦[wait][erase=1]"));
        var r=runtime("e[color=#ff0000]\u0301[/color]");r.advance(10,e->{});assertEquals(1,r.snapshot().glyphs().size());assertEquals("e\u0301",r.snapshot().text());
        var word=runtime("[reveal=word]hel[color=#ff0000]lo[/color] world");List<SubtitleRuntimeEvent> words=new ArrayList<>();word.advance(10,words::add);assertEquals(List.of("hello","world"),words.stream().filter(e->e.kind()==SubtitleRuntimeEvent.Kind.WORD_SHOWN).map(SubtitleRuntimeEvent::unit).toList());
    }
    @Test void pauseStartsAfterPreviousRevealCompletes() {
        var r = runtime("[speed=5]你好[pause=1]世界"); List<SubtitleRuntimeEvent> events = new ArrayList<>(); r.advance(10, events::add);
        var pause = events.stream().filter(e -> e.kind() == SubtitleRuntimeEvent.Kind.PAUSE_STARTED).findFirst().orElseThrow();
        assertTrue(pause.time() >= .3);
        assertTrue(events.stream().noneMatch(e -> e.kind() == SubtitleRuntimeEvent.Kind.REVEAL_UNIT_SHOWN && e.time() > pause.time() && e.time() < pause.time()+1));
    }
    @Test void replaceClearAndEraseStayInsideEntry() {
        assertEquals("欢迎回来，罪人。", rendered("欢迎回来，勇者。[pause=1][replace]欢迎回来，罪人。"));
        assertEquals("罪人。", rendered("勇者[wait][erase=2]罪人。"));
        assertEquals("后文", rendered("前文[clear]后文"));
        assertEquals("后文", rendered("前文[erase=9999]后文"));
    }
    @Test void holdTakesMaximumAndWaitIncludesFiniteEntry() {
        var plan = new SubtitleTimeline().compile(parser.parse("[reveal=instant]a[wait][hold=7][hold=5]b"), EngineConfig.DEFAULT);
        assertEquals(7, plan.holdDuration()); assertTrue(plan.completeAt() >= EngineConfig.DEFAULT.entryDuration());
    }
    @Test void animationsAndEventsAreFrameRateIndependent() {
        List<SubtitleRuntimeEvent> expected = null;
        for (int fps : List.of(30,60,144,240)) {
            var r = runtime("[speed=15]潮水正在上涨……[wait][pause=0.5]\\n快走。"); List<SubtitleRuntimeEvent> events = new ArrayList<>();
            for (int i=0; i<=fps*5; i++) r.advance(i/(double)fps, events::add);
            assertEquals("潮水正在上涨……\n快走。", r.snapshot().text());
            if (expected == null) expected = events; else assertEquals(expected, events);
            int count = events.size(); r.advance(6, events::add); assertEquals(count, events.size());
        }
    }
    @Test void cumulativePauseAndSlowRevealAreBounded() {
        var r = runtime(("[pause=10]").repeat(100) + "终点"); assertTrue(r.completeAt() <= 120); r.advance(121,e->{}); assertEquals("终点", r.snapshot().text());
        var slow = runtime("[speed=1]" + "字".repeat(200)); assertTrue(slow.completeAt() <= 120); slow.advance(121,e->{}); assertEquals(200, slow.snapshot().glyphs().size());
    }
    @Test void attacksHaveBoundedDataAndLiteralFallback() {
        String deep = "[shake]".repeat(100)+"text"+"[/shake]".repeat(100); assertDoesNotThrow(() -> rendered(deep));
        var doc = parser.parse("[br]".repeat(3000)); assertFalse(doc.diagnostics().isEmpty());
        var longDoc = parser.parse("a".repeat(40000)); assertEquals(32768, longDoc.source().text().length());
        Random random = new Random(301);
        for (int n=0;n<2000;n++) { StringBuilder text = new StringBuilder(); for(int i=0;i<200;i++) text.append("[]\\/=abc01".charAt(random.nextInt(10))); assertDoesNotThrow(() -> parser.parse(text.toString())); }
    }
    @Test void queueOverflowAndIdenticalMessagesPreserveIdentity() {
        var queue = new SubtitleQueue(EngineConfig.DEFAULT);
        for(int i=0;i<100;i++) queue.enqueue(new SubtitleQueue.Pending(new ChatEnvelope(i,ChatSource.SYSTEM,SpeakerIdentity.unknown(ChatSource.SYSTEM),StyledText.plain("same"),null,"sig"+i),parser.parse("same"),SubtitleQueue.Priority.NORMAL,null));
        assertEquals(50,queue.queuedSize()); queue.advance(0,e->{}); assertEquals(3,queue.visible().size()); assertEquals(50,queue.visible().getFirst().pending().envelope().id());
        queue.removeSignature("sig51",e->{}); assertEquals(2,queue.visible().size());
    }
    @Test void queuedEntriesPromoteAtLogicalEndAcrossFrameRates(){
        record Event(long id,SubtitleRuntimeEvent.Kind kind,double at,String unit){}
        List<Event> expected=null;
        for(int fps:List.of(30,60,144,240)){
            var queue=new SubtitleQueue(EngineConfig.DEFAULT);List<Event> events=new ArrayList<>();
            for(int id=0;id<12;id++){
                String script="[speed=17]"+"字".repeat(id%4+1)+"[pause=0.13]继续";
                queue.enqueue(new SubtitleQueue.Pending(new ChatEnvelope(id,ChatSource.SYSTEM,SpeakerIdentity.unknown(ChatSource.SYSTEM),StyledText.plain(script),null,null),parser.parse(script),SubtitleQueue.Priority.NORMAL,null),0);
            }
            for(int frame=0;frame<=fps*30;frame++)queue.advance(frame/(double)fps,event->{
                double start=queue.visible().stream().filter(e->e.pending().envelope().id()==event.entryId()).findFirst().orElseThrow().startedAt();
                events.add(new Event(event.entryId(),event.kind(),start+event.time(),event.unit()));
            });
            assertTrue(queue.visible().isEmpty());assertEquals(0,queue.queuedSize());
            if(expected==null)expected=events;else assertEquals(expected,events,"Queue event order at "+fps+" FPS");
        }
        var late=new SubtitleQueue(EngineConfig.DEFAULT);
        late.enqueue(new SubtitleQueue.Pending(new ChatEnvelope(1,ChatSource.SYSTEM,SpeakerIdentity.unknown(ChatSource.SYSTEM),StyledText.plain("late"),null,null),parser.parse("late"),SubtitleQueue.Priority.NORMAL,null),10);
        late.advance(9,e->{});assertTrue(late.visible().isEmpty());late.advance(10,e->{});assertEquals(10,late.visible().getFirst().startedAt());
    }
    @Test void layoutUsesPixelsAndBreaksLongWords() {
        FontBridge font = new FontBridge() { public double width(String s,TextStyle st){return s.equals("宽")?12:6;} public double lineHeight(TextStyle st){return 9;} };
        var r=runtime("宽宽abcdefghij");r.advance(10,e->{});
        var layout=new SubtitleLayoutEngine().layout(r.snapshot(),font,24,SubtitleLayoutEngine.Alignment.CENTER,2);
        assertTrue(layout.height()>30); assertTrue(layout.width()<=24);
    }
    @Test void longLayoutsKeepFirstAndLatestRevealedRowsInsideABoundedViewport(){
        var r=runtime("[speed=120]"+"长文本".repeat(1000));r.advance(0,e->{});
        FontBridge font=new FontBridge(){public double width(String s,TextStyle st){return 9*st.scale();}public double lineHeight(TextStyle st){return 9*st.scale();}};
        var layout=new SubtitleLayoutEngine().layout(r.snapshot(),font,90,SubtitleLayoutEngine.Alignment.LEFT,2);
        assertTrue(layout.height()>1000);var first=SubtitleViewport.at(layout,.1,9,55);assertEquals(55,first.height());assertEquals(0,first.scroll());assertEquals(0,first.first());
        var later=SubtitleViewport.at(layout,10,9,55);assertTrue(later.scroll()>0);assertTrue(later.end()-later.first()<=50);
        var last=layout.glyphs().get(later.end()-1);assertTrue(last.y()-later.scroll()+9<=later.height()+1e-7);
        assertTrue(layout.glyphs().get(later.first()).y()>=later.scroll()-1e-7);assertEquals(1,r.snapshot().glyphs().size(),"Viewport must not advance runtime events");
    }
    @Test void emptyAndInstantViewportsHandleStylesAndShortLayouts(){
        var empty=new SubtitleLayoutEngine.Layout(List.of(),0,0);assertEquals(new SubtitleViewport.Window(0,0,0,0),SubtitleViewport.at(empty,0,9,50));
        var r=runtime("[reveal=instant][scale=2]大字[/scale]\\n尾行");r.advance(0,e->{});
        FontBridge font=new FontBridge(){public double width(String s,TextStyle st){return 9*st.scale();}public double lineHeight(TextStyle st){return 9*st.scale();}};
        var layout=new SubtitleLayoutEngine().layout(r.snapshot(),font,90,SubtitleLayoutEngine.Alignment.LEFT,2);var window=SubtitleViewport.at(layout,0,9,100);
        assertEquals(layout.height(),window.height());assertEquals(0,window.scroll());assertEquals(layout.glyphs().size(),window.end());
    }
    @Test void interpolationDoesNotAdvanceEventsOrCrossDestructiveActions(){
        var r=runtime("[reveal=instant]old[pause=1][replace]new");var events=new ArrayList<SubtitleRuntimeEvent>();r.advance(0,events::add);int count=events.size();
        assertEquals("old",r.snapshotAt(.02).text());assertTrue(r.snapshotAt(.02).entryProgress()>r.snapshot().entryProgress());
        assertTrue(r.snapshotAt(100).time()<2);assertEquals(count,events.size());assertEquals(0,r.snapshot().time());r.advance(2,events::add);assertEquals("new",r.snapshot().text());
    }
}
