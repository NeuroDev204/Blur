"""
Colored Logger Module
Logging với màu sắc cho terminal
"""

import logging
import sys
from typing import Optional


class Colors:
    """ANSI color codes"""
    RESET = '\033[0m'
    BOLD = '\033[1m'
    
    BLACK = '\033[30m'
    RED = '\033[31m'
    GREEN = '\033[32m'
    YELLOW = '\033[33m'
    BLUE = '\033[34m'
    MAGENTA = '\033[35m'
    CYAN = '\033[36m'
    WHITE = '\033[37m'
    
    BRIGHT_BLACK = '\033[90m'
    BRIGHT_RED = '\033[91m'
    BRIGHT_GREEN = '\033[92m'
    BRIGHT_YELLOW = '\033[93m'
    BRIGHT_BLUE = '\033[94m'
    BRIGHT_MAGENTA = '\033[95m'
    BRIGHT_CYAN = '\033[96m'
    BRIGHT_WHITE = '\033[97m'
    
    BG_RED = '\033[41m'
    BG_GREEN = '\033[42m'
    BG_YELLOW = '\033[43m'
    BG_BLUE = '\033[44m'
    BG_MAGENTA = '\033[45m'
    BG_CYAN = '\033[46m'


class ColoredFormatter(logging.Formatter):
    """Custom formatter with colors"""
    
    FORMATS = {
        logging.DEBUG: Colors.BRIGHT_BLACK + "%(asctime)s [DEBUG] %(name)s: %(message)s" + Colors.RESET,
        logging.INFO: Colors.BRIGHT_CYAN + "%(asctime)s [INFO] %(name)s: %(message)s" + Colors.RESET,
        logging.WARNING: Colors.BRIGHT_YELLOW + "%(asctime)s [WARNING] %(name)s: %(message)s" + Colors.RESET,
        logging.ERROR: Colors.BRIGHT_RED + "%(asctime)s [ERROR] %(name)s: %(message)s" + Colors.RESET,
        logging.CRITICAL: Colors.BG_RED + Colors.WHITE + "%(asctime)s [CRITICAL] %(name)s: %(message)s" + Colors.RESET,
    }
    
    def format(self, record):
        log_fmt = self.FORMATS.get(record.levelno)
        formatter = logging.Formatter(log_fmt, datefmt='%H:%M:%S')
        return formatter.format(record)


def get_logger(name: str, level: Optional[str] = None) -> logging.Logger:
    """
    Get logger với colored output
    
    Args:
        name: Logger name
        level: Log level (DEBUG, INFO, WARNING, ERROR, CRITICAL)
    
    Returns:
        Configured logger
    """
    logger = logging.getLogger(name)
    
    # Avoid adding handlers multiple times
    if logger.handlers:
        return logger
    
    # Set level
    if level:
        logger.setLevel(getattr(logging, level.upper(), logging.INFO))
    else:
        logger.setLevel(logging.INFO)
    
    # Console handler with colors
    console_handler = logging.StreamHandler(sys.stdout)
    console_handler.setFormatter(ColoredFormatter())
    logger.addHandler(console_handler)
    
    return logger


# Helper print functions
def print_header(text: str, color=Colors.BRIGHT_MAGENTA):
    """Print a fancy header"""
    print(f"\n{color}{'='*70}")
    print(f"{text:^70}")
    print(f"{'='*70}{Colors.RESET}\n")


def print_subheader(text: str, color=Colors.BRIGHT_CYAN):
    """Print a subheader"""
    print(f"\n{color}{'─'*70}")
    print(f"  {text}")
    print(f"{'─'*70}{Colors.RESET}")


def print_success(text: str):
    """Print success message"""
    print(f"{Colors.BRIGHT_GREEN}✅ {text}{Colors.RESET}")


def print_error(text: str):
    """Print error message"""
    print(f"{Colors.BRIGHT_RED}❌ {text}{Colors.RESET}")


def print_warning(text: str):
    """Print warning message"""
    print(f"{Colors.BRIGHT_YELLOW}⚠️  {text}{Colors.RESET}")


def print_info(text: str):
    """Print info message"""
    print(f"{Colors.BRIGHT_BLUE}ℹ️  {text}{Colors.RESET}")


def print_stat(label: str, value, color=Colors.BRIGHT_WHITE):
    """Print a stat line"""
    print(f"{Colors.BRIGHT_BLACK}   {label}:{Colors.RESET} {color}{value}{Colors.RESET}")
