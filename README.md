
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

## Configuration

Edit `plugins/DaisyVotes/config.yml`:

```yaml
# Individual Voting Rewards
voting:
  message: "<#1aff1a>Thank you for voting! <#ff1a1a>Here are your rewards."
  rewards:
    - "give %player% diamond 5"
    - "eco give %player% 1000"
    - "crate key give %player% vote 1"

# Vote Party System
voteparty:
  message: "<gradient:#1aff1a:#ff1a1a>🎉 Vote Party! Everyone gets rewards!</gradient>"
  totalvotes: 25
  # "individual" = each online player gets rewards
  # "server-wide" = commands run once for entire server
  rewardType: "individual"
  rewards:
    - "give %player% diamond 10"
    - "eco give %player% 5000"
    - "crate key give %player% vote 3"
```

## Placeholders

Requires [PlaceholderAPI](https://www.spigotmc.org/resources/placeholderapi.6245/)

- `%daisyvotes_current_votes%` - Current vote count
- `%daisyvotes_total_votes_needed%` - Votes needed for party
- `%daisyvotes_votes_remaining%` - Votes remaining until party

## Commands

- `/daisyvotes reload` - Reload configuration (permission: `daisyvotes.reload`)

## Building

```bash
git clone https://github.com/fu3i0n/DaisyVotes.git
cd DaisyVotes
./gradlew build
```

## Dependencies

- Paper 1.21+ or Spigot 1.21+
- Votifier (required)
- PlaceholderAPI (optional)

## License

MIT License - see [LICENSE](LICENSE) file
