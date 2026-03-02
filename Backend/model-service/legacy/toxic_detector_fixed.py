"""
Vietnamese Toxic Comment Detector - FIXED
✅ Phát hiện toxic KHÔNG CẦN word boundaries
✅ Xử lý teencode variants (dm, đm, đmmm, đjt, etc.)
✅ Detect toxic dính liền (vãilồn, clmm, etc.)
"""

import re
from typing import List, Dict


class VietnameseToxicDetector:
    """Rule-based Vietnamese toxic comment detector - OPTIMIZED"""
    
    def __init__(self):
        """Initialize detector với toxic keywords + variants"""
        
        # =====================================================================
        # TOXIC KEYWORDS - Cải tiến với VARIANTS
        # =====================================================================
        self.toxic_keywords = {
            # Tục tĩu / Vulgar - với variants
            'vulgar': [
                # Core words
                'đụ', 'địt', 'lồn', 'cặc', 'buồi', 'nứng', 'đéo', 'vãi',
                
                # Teencode variants (CRITICAL!)
                'đjt', 'dit', 'lon', 'cac', 'buoi', 'nung', 'deo', 'vai',
                'dm', 'đm', 'đmmm', 'dmmm', 'dmm', 'đmm',
                'cc', 'ccc', 'ccmm', 'ckmm',
                'll', 'lll', 'llmm', 'clmm',
                'dcm', 'đcm', 'đkm', 'dkm',
                'vcl', 'vl', 'vll', 'vlll', 'cl', 'cll',
                'lol', 'loz', 'lờ', 'loz',
                
                # Compound variants
                'đml', 'dml', 'cml', 'vln', 'đkm', 'ckm',
                'vãi', 'vail', 'vaiz', 'vãiz',
                
                # Animals as insults
                'chó', 'cho', 'súc vật', 'suc vat', 'súc sinh', 
                'con lợn', 'lợn', 'lon', 'lon loi',
                
                # Death wishes
                'chết đi', 'chet di', 'chếtđi', 'chetdi',
                
                # English
                'fuck', 'shit', 'bitch', 'pussy', 'dick', 'ass', 'damn', 'fck', 'wtf'
            ],
            
            # Xúc phạm / Insult
            'insult': [
                'ngu', 'ngốc', 'ngoc', 'khùng', 'khung', 'điên', 'dien', 
                'đần', 'dan', 'óc chó', 'oc cho', 'não tôm', 'nao tom',
                'mất dạy', 'mat day', 'vô học', 'vo hoc',
                'thằng ngu', 'thang ngu', 'con ngu',
                'thằng khốn', 'thang khon', 'đồ khốn', 'do khon',
                'thằng rác', 'thang rac', 'đồ rác', 'do rac', 'rác rưởi', 'rac ruoi',
                'ngu học', 'ngu hoc', 'ngu dốt', 'ngu dot',
                'đồ ngớ ngẩn', 'do ngo ngan', 'hâm', 'ham', 'lười biếng',
                'con ranh', 'thằng ranh', 'đồ ranh', 'ranh con',
                
                # English
                'idiot', 'stupid', 'dumb', 'moron', 'retard', 'loser', 'noob', 'trash'
            ],
            
            # Kỳ thị / Discrimination
            'discrimination': [
                'đồ đồng tính', 'do dong tinh', 'đồ gay', 'do gay', 
                'đồ bóng', 'do bong', 'đồ lộ', 'do lo',
                'thằng đồng', 'thang dong',
                'con gái điếm', 'con gai diem', 'đĩ', 'di', 'cave', 'cavediem',
                'mại dâm', 'mai dam', 'gái gọi', 'gai goi', 
                'con điếm', 'con diem', 'đồ điếm', 'do diem',
                'người mô', 'nguoi mo', 'người rừng', 'nguoi rung',
                'thằng da đen', 'thang da den', 'nô lệ', 'no le'
            ],
            
            # Đe dọa / Threat
            'threat': [
                'giết', 'giet', 'giết mày', 'giet may',
                'chém', 'chem', 'chém mày', 'chem may',
                'đánh', 'danh', 'đập', 'dap',
                'hãm', 'ham', 'hiếp', 'hiep', 'cưỡng hiếp', 'cuong hiep',
                'giở trò', 'gio tro', 'biến đi', 'bien di',
                'cút đi', 'cut di', 'đi chết', 'di chet',
                'chết mẹ', 'chet me', 'chết cha', 'chet cha',
                
                # English
                'kill', 'die', 'death', 'murder'
            ],
            
            # Spam / All caps
            'spam': [
                'sub đi', 'sub di', 'sub nha', 'subscribe',
                'đăng ký kênh', 'dang ky kenh', 'quảng cáo', 'quang cao', 'spam'
            ]
        }
        
        # FAMILY INSULTS
        self.family_insults = [
            'mẹ', 'me', 'má', 'ma', 'cha', 'ba', 'bố', 'bo', 
            'ông', 'ong', 'bà', 'ba', 'cụ', 'cu',
            'con', 'cháu', 'chau', 'anh', 'chị', 'chi', 'em',
            'thằng', 'thang', 'đứa', 'dua',
            'nhà', 'nha', 'tổ tiên', 'to tien', 'ông bà', 'ong ba', 
            'gia đình', 'gia dinh'
        ]
        
        # Compile patterns
        self._compile_patterns()
    
    def _compile_patterns(self):
        """
        Compile regex patterns - KHÔNG dùng word boundaries
        Cho phép match bất kỳ đâu trong text
        """
        self.patterns = {}
        
        for category, keywords in self.toxic_keywords.items():
            # ❌ KHÔNG dùng \b (word boundary)
            # ✅ Chỉ dùng alternation
            pattern = '(' + '|'.join(re.escape(kw) for kw in keywords) + ')'
            self.patterns[category] = re.compile(pattern, re.IGNORECASE)
        
        # Family insult pattern
        family_pattern = '(' + '|'.join(re.escape(word) for word in self.family_insults) + ')'
        self.family_pattern = re.compile(family_pattern, re.IGNORECASE)
    
    def normalize_text(self, text: str) -> str:
        """
        Normalize text - CẢI TIẾN
        """
        # Lowercase
        text = text.lower()
        
        # Remove extra spaces
        text = re.sub(r'\s+', ' ', text)
        
        # ✅ KHÔNG normalize quá mức
        # Giữ nguyên để detect variants như "đmmm", "clmm"
        
        # Remove emoji/special chars nhưng GIỮ chữ
        # text = re.sub(r'[^\w\s]', ' ', text)
        
        return text.strip()
    
    def detect_toxic_categories(self, text: str) -> List[str]:
        """Phát hiện các loại toxic - CẢI TIẾN"""
        normalized = self.normalize_text(text)
        categories = []
        
        # Check each category
        for category, pattern in self.patterns.items():
            matches = pattern.findall(normalized)
            if matches:
                categories.append(category)
                # Debug
                # print(f"  [{category}] matched: {matches[:3]}")
        
        # Check family insults (kết hợp với toxic)
        family_matches = self.family_pattern.findall(normalized)
        if family_matches:
            # Phải có toxic word kèm theo
            has_toxic = any(pattern.search(normalized) for pattern in self.patterns.values())
            if has_toxic:
                categories.append('family_insult')
        
        return categories
    
    def calculate_toxicity_score(self, text: str, categories: List[str]) -> float:
        """Tính điểm toxic (0.0 - 1.0)"""
        if not categories:
            return 0.0
        
        # Base score
        score = 0.3
        
        # Category weights
        weights = {
            'vulgar': 0.30,      # ⬆ Tăng
            'insult': 0.25,      # ⬆ Tăng
            'discrimination': 0.35,
            'threat': 0.45,
            'spam': 0.10,
            'family_insult': 0.55  # ⬆ Tăng
        }
        
        # Add scores
        for cat in categories:
            score += weights.get(cat, 0.15)
        
        # Multiple toxic words
        normalized = self.normalize_text(text)
        toxic_count = sum(
            len(pattern.findall(normalized))
            for pattern in self.patterns.values()
        )
        
        if toxic_count > 1:
            score += min(0.25, toxic_count * 0.06)  # ⬆ Tăng
        
        # All caps (SCREAMING)
        if text.isupper() and len(text) > 10:
            score += 0.15  # ⬆ Tăng
        
        # Excessive punctuation (!!!, ???)
        if len(re.findall(r'[!?]{3,}', text)) > 0:
            score += 0.08  # ⬆ Tăng
        
        return min(1.0, score)
    
    def is_toxic(self, text: str, threshold: float = 0.45) -> bool:
        """
        Kiểm tra toxic - THRESHOLD GIẢM từ 0.5 → 0.45
        Để catch được nhiều case hơn
        """
        categories = self.detect_toxic_categories(text)
        score = self.calculate_toxicity_score(text, categories)
        
        return score >= threshold
    
    def analyze_comment(self, comment: str) -> Dict:
        """Phân tích đầy đủ comment"""
        categories = self.detect_toxic_categories(comment)
        score = self.calculate_toxicity_score(comment, categories)
        is_toxic = score >= 0.45  # ⬇ Giảm threshold
        
        return {
            'text': comment,
            'is_toxic': is_toxic,
            'toxicity_score': score,
            'toxic_categories': categories,
            'length': len(comment)
        }
    
    def label_comments_batch(self, comments: List[Dict]) -> List[Dict]:
        """Label một batch comments"""
        labeled = []
        
        for comment in comments:
            text = comment.get('text', '')
            
            if not text or len(text) < 3:
                # Skip empty
                labeled.append({
                    **comment,
                    'is_toxic': False,
                    'toxicity_score': 0.0,
                    'toxic_categories': []
                })
                continue
            
            # Analyze
            analysis = self.analyze_comment(text)
            
            # Merge
            labeled_comment = {**comment, **analysis}
            labeled.append(labeled_comment)
        
        return labeled


# ============================================================================
# TEST
# ============================================================================
if __name__ == "__main__":
    detector = VietnameseToxicDetector()
    
    # Test cases - TIKTOK STYLE (không dấu cách, teencode)
    test_comments = [
        "Video hay quá bạn ơi!",
        "đmmm video gì mà rác thế",
        "Đồ ngu, không biết gì cả",
        "vãilồn",
        "clmm",
        "thằng này ngu vl",
        "Mày là thằng đần à",
        "Chết mẹ mày đi",
        "vcl ông này",
        "dm thằng ngu",
        "Bạn làm tốt lắm!",
        "con chó này",
        "deo biet gi ca",
        "đéo hiểu",
        "ngu vcl",
        "AAAAAAA !!!!!!",
        "sub đi các bạn",
        "lol rác"
    ]
    
    print("\n" + "="*70)
    print("VIETNAMESE TOXIC DETECTOR - FIXED VERSION")
    print("="*70)
    
    toxic_count = 0
    
    for i, comment in enumerate(test_comments, 1):
        result = detector.analyze_comment(comment)
        
        status = "🔴 TOXIC" if result['is_toxic'] else "🟢 CLEAN"
        toxic_count += result['is_toxic']
        
        print(f"\n{i}. {status}")
        print(f"   Text: {comment}")
        print(f"   Score: {result['toxicity_score']:.3f}")
        if result['toxic_categories']:
            print(f"   Categories: {', '.join(result['toxic_categories'])}")
    
    print("\n" + "="*70)
    print(f"Summary: {toxic_count}/{len(test_comments)} toxic comments detected")
    print(f"Detection rate: {toxic_count/len(test_comments)*100:.1f}%")
    print("="*70)