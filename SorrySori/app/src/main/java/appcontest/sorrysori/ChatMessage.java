package appcontest.sorrysori;

class ChatMessage {  // 채팅할 때 필요한 사용자의 정보
    String id;
    String text;
    String name;
    String photoUrl;

    ChatMessage(String text, String name, String photoUrl) { //생성자를 이용하여 초기화,
        this.text = text;
        this.name = name;
        this.photoUrl = photoUrl;
    }

    ChatMessage() {

    }
}