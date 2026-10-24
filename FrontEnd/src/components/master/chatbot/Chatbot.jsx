import React, { useState, useEffect, useRef } from 'react';
import { assistantService } from '../../../services/assistantService';
import './Chatbot.css';

const getCurrentTime = () => {
  const now = new Date();
  const hours = String(now.getHours()).padStart(2, '0');
  const minutes = String(now.getMinutes()).padStart(2, '0');
  return `${hours}:${minutes}`;
};

const escapeHTML = (str) => {
  return str.replace(/[&<>'"]/g,
    tag => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', "'": '&#39;', '"': '&quot;' }[tag] || tag)
  );
};

/**
 * Renders a message as safe HTML. Everything is HTML-escaped FIRST (AI replies may echo user input),
 * then only a small subset of Markdown (Gemini's usual output) is turned back into tags.
 */
const formatMessage = (text) =>
  escapeHTML(text)
    .replace(/^#{1,6}\s+(.+)$/gm, '<b>$1</b>')               // headings -> bold line
    .replace(/^\s*[*-]\s+/gm, '• ')                          // bullet lists
    .replace(/\*\*(.+?)\*\*/g, '<b>$1</b>')                  // **bold**
    .replace(/(^|[^*])\*(\S(?:[^*\n]*\S)?)\*(?!\*)/g, '$1<i>$2</i>') // *italic* (no spaces inside the markers, so "2 * 3" stays)
    .replace(/`([^`\n]+)`/g, '<code>$1</code>')              // `code`
    .replace(/\n/g, '<br>');

const welcomeMessage = (content) => ({
  id: Date.now(),
  type: 'bot',
  content,
  time: getCurrentTime(),
  showQuickChips: true
});

// Maps an API failure to a reply shown inside the conversation
const errorReply = (response) => {
  if (response?.status === 401) {
    return 'Vui lòng đăng nhập để trò chuyện với Trợ lý AI.';
  }
  return response?.data?.message || 'Trợ lý AI tạm thời không phản hồi. Vui lòng thử lại sau.';
};

const Chatbot = () => {
  const [isActive, setIsActive] = useState(false);
  const [messages, setMessages] = useState([]);
  const [inputValue, setInputValue] = useState('');
  const [showTyping, setShowTyping] = useState(false);
  const [provider, setProvider] = useState('gemini');
  const chatBodyRef = useRef(null);

  // Initialize with welcome message
  useEffect(() => {
    setMessages([welcomeMessage('Xin chào! 👋 Tôi là Trợ lý AI. Tôi có thể giúp gì cho bạn hôm nay?')]);
  }, []);

  // Scroll to bottom when messages change
  useEffect(() => {
    if (chatBodyRef.current) {
      chatBodyRef.current.scrollTop = chatBodyRef.current.scrollHeight;
    }
  }, [messages, showTyping]);

  const toggleChat = () => {
    setIsActive(!isActive);
  };

  const clearChat = () => {
    setMessages([welcomeMessage('Lịch sử hội thoại đã được làm mới! 👋 Tôi có thể giúp gì cho bạn?')]);
  };

  const addMessage = (type, content, extra = {}) => {
    setMessages(prev => [
      ...prev,
      { id: `${Date.now()}-${prev.length}`, type, content, time: getCurrentTime(), ...extra }
    ]);
  };

  const handleSendMessage = async (text) => {
    const message = text.trim();
    // One request at a time: a reply can take several seconds
    if (!message || showTyping) return;

    addMessage('user', message);
    setInputValue('');
    setShowTyping(true);

    try {
      const response = await assistantService.chat(message, provider);
      if (response.ok) {
        addMessage('bot', response.data?.data?.reply || '');
      } else {
        addMessage('bot', errorReply(response), { isError: true });
      }
    } catch {
      addMessage('bot', 'Không thể kết nối tới máy chủ. Vui lòng kiểm tra mạng và thử lại.', { isError: true });
    } finally {
      setShowTyping(false);
    }
  };

  const handleQuickMessage = (text) => {
    handleSendMessage(text);
  };

  const handleKeyDown = (e) => {
    // isComposing: ignore Enter while an IME (e.g. Vietnamese Telex) is still composing a character
    if (e.key === 'Enter' && !e.nativeEvent.isComposing) {
      e.preventDefault();
      handleSendMessage(inputValue);
    }
  };

  return (
    <div className={`chatbot-widget ${isActive ? 'active' : ''}`}>
      {/* Floating Toggle Button */}
      <button
        className="chatbot-toggle"
        onClick={toggleChat}
        aria-label="Toggle Chat"
      >
        <div className="pulse-ring"></div>

        {/* Chat Icon with stars */}
        <div className="icon-chat">
          <i className="fa-solid fa-comments"></i>
          <svg className="star-sparkle star-1" viewBox="0 0 24 24">
            <path d="M12 0L14.59 9.41L24 12L14.59 14.59L12 24L9.41 14.59L0 12L9.41 9.41L12 0Z"/>
          </svg>
          <svg className="star-sparkle star-2" viewBox="0 0 24 24">
            <path d="M12 0L14.59 9.41L24 12L14.59 14.59L12 24L9.41 14.59L0 12L9.41 9.41L12 0Z"/>
          </svg>
          <svg className="star-sparkle star-3" viewBox="0 0 24 24">
            <path d="M12 0L14.59 9.41L24 12L14.59 14.59L12 24L9.41 14.59L0 12L9.41 9.41L12 0Z"/>
          </svg>
        </div>

        <i className="fa-solid fa-xmark icon-close"></i>
        <span className="notification-badge">1</span>
      </button>

      {/* Expandable Chat Window */}
      <div className="chat-window">
        {/* Header */}
        <div className="chat-header">
          <div className="bot-info">
            <div className="avatar-container">
              <img
                src="https://images.unsplash.com/photo-1618005182384-a83a8bd57fbe?auto=format&fit=crop&w=120&q=80"
                alt="AI Avatar"
                className="avatar-img"
              />
              <span className="status-dot"></span>
            </div>
          </div>
          <div className="provider-selector" aria-label="Chọn AI provider">
            <button
              type="button"
              className={provider === 'gemini' ? 'active' : ''}
              onClick={() => setProvider('gemini')}
              disabled={showTyping}
            >
              Gemini
            </button>
            <button
              type="button"
              className={provider === 'groq' ? 'active' : ''}
              onClick={() => setProvider('groq')}
              disabled={showTyping}
            >
              GPT
            </button>
          </div>
          <div className="header-actions">
            <button
              className="header-btn"
              onClick={clearChat}
              title="Xóa lịch sử chat"
            >
              <i className="fa-solid fa-rotate-right"></i>
            </button>
            <button
              className="header-btn"
              onClick={toggleChat}
              title="Đóng"
            >
              <i className="fa-solid fa-chevron-down"></i>
            </button>
          </div>
        </div>

        {/* Scrollable Message Body */}
        <div className="chat-body" ref={chatBodyRef}>
          {messages.map((msg) => (
            <div key={msg.id} className={`message-row ${msg.type}${msg.isError ? ' error' : ''}`}>
              {msg.type === 'bot' && (
                <div className="msg-avatar">
                  <i className="fa-solid fa-robot"></i>
                </div>
              )}
              <div className="msg-content">
                <div
                  className="msg-bubble"
                  // Safe: formatMessage escapes all HTML before adding its own tags
                  dangerouslySetInnerHTML={{ __html: formatMessage(msg.content) }}
                />
                {msg.showQuickChips && (
                  <div className="quick-chips">
                    <button
                      className="chip-btn"
                      onClick={() => handleQuickMessage('Sản phẩm nổi bật')}
                      disabled={showTyping}
                    >
                      🔥 Sản phẩm nổi bật
                    </button>
                    <button
                      className="chip-btn"
                      onClick={() => handleQuickMessage('Chính sách đổi trả')}
                      disabled={showTyping}
                    >
                      📦 Chính sách đổi trả
                    </button>
                    <button
                      className="chip-btn"
                      onClick={() => handleQuickMessage('Tư vấn cấu hình')}
                      disabled={showTyping}
                    >
                      💻 Tư vấn cấu hình
                    </button>
                  </div>
                )}
                <span className="msg-time">{msg.time}</span>
              </div>
            </div>
          ))}

          {/* Typing Indicator */}
          {showTyping && (
            <div className="message-row bot">
              <div className="msg-avatar">
                <i className="fa-solid fa-robot"></i>
              </div>
              <div className="typing-indicator">
                <div className="typing-dot"></div>
                <div className="typing-dot"></div>
                <div className="typing-dot"></div>
              </div>
            </div>
          )}
        </div>

        {/* Footer / Input Controls */}
        <div className="chat-footer">
          <div className="input-wrapper">
            <input
              type="text"
              className="chat-input"
              placeholder="Nhập tin nhắn..."
              value={inputValue}
              onChange={(e) => setInputValue(e.target.value)}
              onKeyDown={handleKeyDown}
              maxLength={4000}
              autoComplete="off"
            />
            <button
              className="attach-btn"
              title="Đính kèm tệp"
              onClick={() => alert('Tính năng tải tệp đang phát triển!')}
            >
              <i className="fa-solid fa-paperclip"></i>
            </button>
          </div>
          <button
            className="send-btn"
            onClick={() => handleSendMessage(inputValue)}
            title="Gửi tin nhắn"
            disabled={!inputValue.trim() || showTyping}
          >
            <i className="fa-solid fa-paper-plane"></i>
          </button>
        </div>
      </div>
    </div>
  );
};

export default Chatbot;
