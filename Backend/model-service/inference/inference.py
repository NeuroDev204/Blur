"""
Bước 3: Inference - Sử Dụng Trained Model
Dùng PhoBERT model đã train để predict toxic comments
"""

import torch
from transformers import AutoTokenizer, AutoModelForSequenceClassification
import json
from datetime import datetime


class ToxicCommentPredictor:
    def __init__(self, model_path):
        """
        Initialize predictor with trained model

        Args:
            model_path (str): Path to trained model directory
        """
        self.device = torch.device('cuda' if torch.cuda.is_available() else 'cpu')

        print(f"🚀 Loading model from: {model_path}")
        print(f"   Device: {self.device}")

        # Load tokenizer and model
        self.tokenizer = AutoTokenizer.from_pretrained(model_path)
        self.model = AutoModelForSequenceClassification.from_pretrained(model_path)
        self.model.to(self.device)
        self.model.eval()

        print(f"   ✅ Model loaded successfully")

    def predict(self, text):
        """
        Predict if a comment is toxic

        Args:
            text (str): Comment text

        Returns:
            dict: Prediction result
        """
        # Tokenize
        inputs = self.tokenizer(
            text,
            padding='max_length',
            truncation=True,
            max_length=256,
            return_tensors='pt'
        )
        inputs = {k: v.to(self.device) for k, v in inputs.items()}

        # Predict
        with torch.no_grad():
            outputs = self.model(**inputs)
            logits = outputs.logits
            probabilities = torch.softmax(logits, dim=1)

        # Get prediction
        predicted_class = torch.argmax(probabilities, dim=1).item()
        confidence = probabilities[0][predicted_class].item()

        # Class 0 = Clean, Class 1 = Toxic
        is_toxic = predicted_class == 1
        toxic_probability = probabilities[0][1].item()
        clean_probability = probabilities[0][0].item()

        return {
            'text': text,
            'is_toxic': is_toxic,
            'predicted_class': predicted_class,
            'confidence': confidence,
            'toxic_probability': toxic_probability,
            'clean_probability': clean_probability
        }

    def predict_batch(self, texts, batch_size=32):
        """
        Predict batch of comments

        Args:
            texts (list): List of comment texts
            batch_size (int): Batch size for processing

        Returns:
            list: Prediction results
        """
        results = []

        for i in range(0, len(texts), batch_size):
            batch_texts = texts[i:i + batch_size]

            # Tokenize batch
            inputs = self.tokenizer(
                batch_texts,
                padding='max_length',
                truncation=True,
                max_length=256,
                return_tensors='pt'
            )
            inputs = {k: v.to(self.device) for k, v in inputs.items()}

            # Predict
            with torch.no_grad():
                outputs = self.model(**inputs)
                logits = outputs.logits
                probabilities = torch.softmax(logits, dim=1)

            # Process results
            for j, text in enumerate(batch_texts):
                predicted_class = torch.argmax(probabilities[j]).item()
                confidence = probabilities[j][predicted_class].item()
                is_toxic = predicted_class == 1

                results.append({
                    'text': text,
                    'is_toxic': is_toxic,
                    'predicted_class': predicted_class,
                    'confidence': confidence,
                    'toxic_probability': probabilities[j][1].item(),
                    'clean_probability': probabilities[j][0].item()
                })

        return results


def test_model_examples(predictor):
    """
    Test model với ví dụ

    Args:
        predictor: ToxicCommentPredictor instance
    """
    print("\n" + "=" * 70)
    print("TEST MODEL WITH EXAMPLES")
    print("=" * 70)

    test_cases = [
        # Clean comments
        "Video này hay quá, cảm ơn bạn đã chia sẻ!",
        "Bài hát rất hay và ý nghĩa",
        "Chúc mừng bạn nhé, xứng đáng lắm!",
        "Mình rất thích kênh của bạn",

        # Toxic comments
        "Đồ ngu, video này tệ vãi",
        "Địt mẹ thằng này ngu vcl",
        "Chết đi con chó",
        "Đồ đĩ, xấu như cứt",

        # Borderline cases
        "Video này không hay lắm",
        "Mình không thích lắm",
        "Hơi nhạt nhẽo",
    ]

    for i, text in enumerate(test_cases, 1):
        result = predictor.predict(text)

        status = "🚫 TOXIC" if result['is_toxic'] else "✅ CLEAN"
        print(f"\n{i}. {status}")
        print(f"   Text: {text}")
        print(f"   Confidence: {result['confidence']:.2%}")
        print(f"   Toxic prob: {result['toxic_probability']:.2%}")
        print(f"   Clean prob: {result['clean_probability']:.2%}")


def predict_youtube_comments(predictor, scraper, video_id):
    """
    Predict toxic comments from YouTube video

    Args:
        predictor: ToxicCommentPredictor instance
        scraper: YouTubeCommentScraper instance
        video_id (str): YouTube video ID

    Returns:
        list: Predictions
    """
    from youtube_comment_scraper import YouTubeCommentScraper

    print(f"\n🎬 Analyzing YouTube video: {video_id}")

    # Get comments
    comments = scraper.get_video_comments(
        video_id=video_id,
        max_results=100,
        vietnamese_only=True
    )

    print(f"   Fetched {len(comments)} comments")

    # Predict
    print(f"   Analyzing toxicity...")
    texts = [c['text'] for c in comments]
    predictions = predictor.predict_batch(texts)

    # Add predictions to comments
    for i, comment in enumerate(comments):
        comment.update(predictions[i])

    # Statistics
    toxic_count = sum(1 for p in predictions if p['is_toxic'])
    toxic_rate = toxic_count / len(predictions) * 100 if predictions else 0

    print(f"\n📊 Results:")
    print(f"   Total comments: {len(predictions)}")
    print(f"   Toxic: {toxic_count} ({toxic_rate:.1f}%)")
    print(f"   Clean: {len(predictions) - toxic_count} ({100-toxic_rate:.1f}%)")

    # Show examples
    print(f"\n🚫 Sample toxic comments:")
    toxic_samples = [p for p in predictions if p['is_toxic']][:3]
    for i, sample in enumerate(toxic_samples, 1):
        print(f"   {i}. {sample['text'][:80]}...")
        print(f"      Confidence: {sample['confidence']:.2%}")

    return comments


def save_predictions(predictions, output_path):
    """
    Save predictions to file

    Args:
        predictions (list): Prediction results
        output_path (str): Output file path
    """
    with open(output_path, 'w', encoding='utf-8') as f:
        json.dump(predictions, f, indent=2, ensure_ascii=False)

    print(f"\n💾 Predictions saved to: {output_path}")


def main():
    """Main inference pipeline"""

    print("=" * 70)
    print("PHOBERT TOXIC COMMENT CLASSIFIER - INFERENCE")
    print("=" * 70)

    # =========================================================================
    # CÀI ĐẶT
    # =========================================================================

    # Path to trained model (from step 2)
    MODEL_PATH = "models/phobert_toxic_20251021_233036"  # Update with your model path

    # =========================================================================
    # LOAD MODEL
    # =========================================================================

    predictor = ToxicCommentPredictor(MODEL_PATH)

    # =========================================================================
    # TEST 1: VÍ DỤ ĐƠN GIẢN
    # =========================================================================

    test_model_examples(predictor)

    # =========================================================================
    # TEST 2: CUSTOM TEXT
    # =========================================================================

    print("\n" + "=" * 70)
    print("TEST YOUR OWN COMMENT")
    print("=" * 70)

    custom_text = "Video này hay lắm, cảm ơn bạn!"  # Thay bằng text của bạn
    result = predictor.predict(custom_text)

    status = "🚫 TOXIC" if result['is_toxic'] else "✅ CLEAN"
    print(f"\n{status}")
    print(f"Text: {custom_text}")
    print(f"Confidence: {result['confidence']:.2%}")

    # =========================================================================
    # TEST 3: YOUTUBE VIDEO (OPTIONAL)
    # =========================================================================

    # Uncomment để test với YouTube comments
    """
    from youtube_comment_scraper import YouTubeCommentScraper

    API_KEY = "YOUR_API_KEY"
    VIDEO_ID = "VIDEO_ID"

    scraper = YouTubeCommentScraper(API_KEY)
    predictions = predict_youtube_comments(predictor, scraper, VIDEO_ID)

    # Save predictions
    timestamp = datetime.now().strftime("%Y%m%d_%H%M%S")
    save_predictions(predictions, f"predictions_{timestamp}.json")
    """

    print("\n" + "=" * 70)
    print("✅ INFERENCE COMPLETE!")
    print("=" * 70)


if __name__ == "__main__":
    main()
