import torch
from transformers import AutoTokenizer, AutoModelForSequenceClassification
from typing import List
from app.schemas import CommentAnalysis, ToxicLevel

class ToxicPredictor:
  """
   PhoBERT-based Vietnamese toxic comment predictor.

    Model: vinai/phobert-base fine-tuned trên dataset toxic comments tiếng Việt.
    Input:  text string (tối đa 256 tokens sau tokenize)
    Output: CommentAnalysis với is_toxic, toxic_score, confidence, probabilities

    Performance: ~50-200ms/comment trên CPU, ~10-30ms trên GPU
  """
  def __init__(self, model_path: str):
    """ load model va tokenizer tu local path"""
    self.device = torch.device('cuda' if torch.cuda.is_available() else 'cpu')
    self.tokenizer = AutoTokenizer.from_pretrained(model_path)
    self.model = AutoModelForSequenceClassification.from_pretrained(model_path)
    self.model.to(self.device)
    self.model.eval()
  def _get_toxic_level(self, score: float) -> ToxicLevel:
    if score < 0.4:
      return ToxicLevel.LOW
    elif score < 0.7:
      return ToxicLevel.MEDIUM
    return ToxicLevel.HIGHT
  def predict(self,text: str) -> CommentAnalysis:
    inputs = self.tokenizer(
      text,
      padding='max_length',
      truncation=True,
      max_length=256,
      return_tensors='pt'
    )
    inputs = {k: v.to(self.device) for k, v in inputs.items()}
    with torch.no_grad():
      outputs = self.model(**inputs)
      probs = torch.softmax(outputs.logits, dim=1)
    toxic_prob = probs[0][1].item()
    clean_prob = probs[0][0].item()
    return CommentAnalysis(
      text=text,
      is_toxic=toxic_prob > 0.5,
      toxic_score=round(toxic_prob,4),
      toxic_level=self._get_toxic_level(toxic_prob),
      confidence=round(max(toxic_prob,clean_prob),4),
      probabilities={"clean": round(clean_prob,4), "toxic": round(toxic_prob,4)}
    )
  def predict_batch(self, texts: List[str]) -> List[CommentAnalysis]:
      inputs = self.tokenizer(
          texts,
          padding='max_length',
          truncation=True,
          max_length=256,
          return_tensors='pt'
      )
      inputs = {k: v.to(self.device) for k, v in inputs.items()}

      with torch.no_grad():
          outputs = self.model(**inputs)
          probs = torch.softmax(outputs.logits, dim=1)

      results = []
      for i, text in enumerate(texts):
          toxic_prob = probs[i][1].item()
          clean_prob = probs[i][0].item()
          results.append(CommentAnalysis(
              text=text,
              is_toxic=toxic_prob > 0.5,
              toxic_score=round(toxic_prob, 4),
              toxic_level=self._get_toxic_level(toxic_prob),
              confidence=round(max(toxic_prob, clean_prob), 4),
              probabilities={"clean": round(clean_prob, 4), "toxic": round(toxic_prob, 4)}
          ))
      return results
  