
# 🎉 DaisyVotes 🎉

DaisyVotes is a **modern, scalable, and feature-rich voting system** built with **Kotlin** and **Java**. Designed to handle everything from small polls to large-scale elections, DaisyVotes is the perfect solution for developers and organizations looking for a reliable and customizable voting system.

---

## ✨ Features

- **Scalable Voting System**: Handles small polls to large-scale elections effortlessly.
- **Vote Party Support**: Reward participants when a vote threshold is reached.
- **Easy Configuration**: Intuitive setup with flexible options.
- **Real-Time Results**: Instantaneous vote tallying and live updates.
- **Secure and Reliable**: Ensures data integrity and tamper-proof results.
- **Open Source**: Built for the community, by the community.

---

## 🛠️ Built With

- **Kotlin & Java**: A modern, high-performance tech stack.
- **Gradle**: Simplified builds and dependency management.
- **Modular Architecture**: Clean, extensible design for easy maintenance and feature additions.

---

## 📦 Installation

Getting started is simple! Clone the repository and build the project using Gradle:

```bash
git clone https://github.com/fu3i0n/DaisyVotes.git
cd DaisyVotes
./gradlew build
```

---

## ⚙️ Configuration

DaisyVotes offers an easy-to-use configuration file to customize your voting system. Example:

```yaml
# Configuration for individual voting rewards
voting:
  # Message sent to the player when they vote
  message: "<#1aff1a>Thank you for voting! <#ff1a1a>Here are your rewards."

  # List of commands executed when a player votes
  # %player% is replaced with the player's name
  rewards:
    - "crate key give %player% vote 1" # Gives the player 1 vote crate key

# Configuration for vote party rewards
voteparty:
  # Message broadcasted to all players when a vote party is triggered
  message: "<#1aff1a>Vote party! <#ff1a1a>Everyone gets a reward."

  # Number of votes required to trigger a vote party
  totalvotes: 25

  # Determines how rewards are distributed:
  # "individual" - Rewards are given to each player individually
  # "server-wide" - Rewards are given to the entire server
  rewardType: "individual"

  # List of commands executed during a vote party
  # %player% is replaced with the player's name (only for "individual" rewardType)
  rewards:
    - "crate key give %player% vote 1" # Gives each player 1 vote crate key
```

---

## 🌍 Why DaisyVotes?

DaisyVotes is designed to empower developers and organizations with a reliable, open-source voting system. Whether you're building a small poll, a vote party system, or a large-scale election, DaisyVotes has you covered.

---

## 🤝 Contributions

We believe in the power of community! Contributions are welcome—whether it's reporting issues, submitting pull requests, or suggesting new features. Together, we can make DaisyVotes even better.

---

## 📜 License

This project is licensed under the MIT License. See the `LICENSE` file for details.

---

## 🔗 Links

- **Repository**: [DaisyVotes on GitHub](https://github.com/fu3i0n/DaisyVotes)
- **Issues**: [Report Issues](https://github.com/fu3i0n/DaisyVotes/issues)

Thank you for your support! 🎉
