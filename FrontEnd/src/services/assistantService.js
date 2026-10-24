// services/assistantService.js
import { apiService } from './apiService';

export const assistantService = {
  // silent: the chatbot shows errors inside the conversation instead of a popup
  chat: (message, provider) =>
    apiService.fetch('/api/assistant/chat', { method: 'POST', body: { message, provider }, silent: true }),
};
