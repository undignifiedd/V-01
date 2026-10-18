package com.Version_1.Story;

/** Story dialogue. Each array entry begins a new page; flower-room wording is still a draft. */
public final class DialogueData {
    private DialogueData() { }
    public static final String[] DEATH_DIARY = {"I tried again.", "Yet I have nothing to show for it"};

    public static final String[] DIARY = {
        "I broke the mirror this morning. For a second, I thought someone was behind me.\n\nIt was only me.",
        "There's a flower on the kitchen table. Someone left it in a glass of water.\n\nI've walked past it six times. I still haven't read the note underneath.",
        "I keep telling myself I'll put everything right when I feel better.\n\nToday I could start with something small."
    };

    public static final String[] NPC_NAMES = {
        "Bob", "Jack", "Lady"
    };
    public static final String[][] NPC_DIALOGUE = {
        {"Studies have shown that HIIIIII"},
        {"They put chips in peanut butter so they can program you to have less kids"},
        {"you know.... i really want to give bob this flower but im too shy",
            "I cant do it",
            "The only way left is for the devil himself to help me"}
    };
    public static final String[] JACK_FLOWER = {
        "Bob? Do i look like a Bob to you? Id be fine being a Stacy or a Rachael but Bob? Seriously?"
    };
    public static final String FLOWER_QUESTION = "Would you like to help the lady give flowers to Bob?";
    public static final String[] AFTER_REVEAL = {
        "There was something I was supposed to do today.\n\nI remember being asked.\n\nI remember thinking it would be easy.",
        "The glass on the kitchen table is empty now.\n\nThere's a pale ring where it stood. I tried wiping it away, but it seems to be underneath the wood.",
        "Someone knocked while I was writing this.\n\nI waited until they stopped.\n\nThen I waited a little longer.",
        "THERE IS SOMETHING INS---"
    };
    public static final String[] FLOWERS = {
        "Only with death do people realize\n the true meaning of life",
        "Begone vile demon\n"
    };
}
