import React, { useState } from 'react';
import { Edit3, Plus, Trash2, Check, X } from 'lucide-react';
import { CurbNote } from '../types';

interface NoteSectionProps {
  note?: CurbNote | null;
  onSaveNote: (text: string) => void;
  onDeleteNote?: () => void;
  title?: string;
  isPro?: boolean;
  onUpgradePro?: () => void;
}

export const NoteSection: React.FC<NoteSectionProps> = ({
  note,
  onSaveNote,
  onDeleteNote,
  title = 'Parking Spot Note',
  isPro = false,
  onUpgradePro
}) => {
  const [isEditing, setIsEditing] = useState(false);
  const [editText, setEditText] = useState(note?.text || '');

  const handleStartEdit = () => {
    setEditText(note?.text || '');
    setIsEditing(true);
  };

  const handleSave = () => {
    if (editText.trim()) {
      onSaveNote(editText.trim());
      setIsEditing(false);
    }
  };

  const handleCancel = () => {
    setEditText(note?.text || '');
    setIsEditing(false);
  };

  return (
    <div className="bg-[#FFFFFF] border border-[#EDE0DC] rounded-3xl p-4 shadow-sm">
      <div className="flex items-center justify-between mb-2">
        <span className="text-xs font-bold uppercase tracking-wider text-[#85736E] flex items-center gap-1.5">
          <Edit3 className="w-3.5 h-3.5 text-[#8F4C38]" />
          {title}
        </span>

        {!isEditing && (
          <button
            onClick={handleStartEdit}
            className="text-xs font-bold text-[#8F4C38] hover:underline flex items-center gap-1"
          >
            {note ? 'Edit' : '+ Add Note'}
          </button>
        )}
      </div>

      {isEditing ? (
        <div className="space-y-2 mt-2">
          <textarea
            value={editText}
            onChange={(e) => setEditText(e.target.value)}
            placeholder="e.g. Parked by the blue mailbox, meter space #412..."
            rows={3}
            className="w-full text-xs font-medium p-3 rounded-2xl border border-[#D6C2BC] bg-[#FDF8F6] text-[#1F1B1A] focus:outline-none focus:ring-2 focus:ring-[#8F4C38]/20 focus:border-[#8F4C38]"
          />
          <div className="flex items-center justify-between">
            {note && onDeleteNote ? (
              <button
                onClick={() => {
                  onDeleteNote();
                  setIsEditing(false);
                }}
                className="text-xs font-bold text-[#BA1A1A] hover:underline flex items-center gap-1"
              >
                <Trash2 className="w-3 h-3" />
                Delete
              </button>
            ) : <div />}

            <div className="flex gap-2">
              <button
                onClick={handleCancel}
                className="px-3 py-1.5 rounded-xl border border-[#EDE0DC] text-xs font-semibold text-[#85736E] hover:bg-[#F3E9E5]"
              >
                Cancel
              </button>
              <button
                onClick={handleSave}
                className="px-3 py-1.5 rounded-xl bg-[#8F4C38] text-white text-xs font-bold shadow-sm hover:bg-[#3A0B01]"
              >
                Save Note
              </button>
            </div>
          </div>
        </div>
      ) : note ? (
        <p className="text-xs font-medium text-[#51433F] bg-[#FDF8F6] p-3 rounded-2xl border border-[#EDE0DC]">
          "{note.text}"
        </p>
      ) : (
        <p className="text-xs text-[#85736E] italic">
          No custom note added. Tap to add details like meter numbers or landmarks.
        </p>
      )}
    </div>
  );
};
