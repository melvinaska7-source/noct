package zov.alphadlc.util.friend;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class CaseInsensitiveNameIndexTest {
    private record Entry(String name) {
    }

    @Test
    void maintainsCaseInsensitiveLookupAndInsertionOrder() {
        CaseInsensitiveNameIndex<Entry> index = new CaseInsensitiveNameIndex<>(Entry::name);

        assertTrue(index.add(new Entry("Alice")));
        assertFalse(index.add(new Entry("aLiCe")));
        assertTrue(index.contains("ALICE"));
        assertEquals("Alice", index.get("alice").name());
        assertEquals(List.of(new Entry("Alice")), index.values());

        assertEquals("Alice", index.remove("aLiCe").name());
        assertFalse(index.contains("alice"));

        index.replaceAll(List.of(new Entry("Bob"), new Entry("BOB"), new Entry("Carol")));
        assertEquals(List.of(new Entry("Bob"), new Entry("Carol")), index.values());
        index.clear();
        assertEquals(List.of(), index.values());
    }
}
